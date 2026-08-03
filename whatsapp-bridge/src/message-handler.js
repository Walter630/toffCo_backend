/**
 * message-handler.js — Recebe mensagens do WhatsApp e repassa pro backend Java.
 *
 * Fluxo:
 * 1. Baileys emite evento 'messages.upsert' (mensagem nova)
 * 2. connection.js chama onMessageReceived (registrado aqui)
 * 3. Este módulo extrai os dados relevantes (número, texto, messageId, fromMe, mídia)
 * 4. Monta um payload NO MESMO FORMATO que a Evolution API mandava
 * 5. Faz POST pro backend Java no endpoint /api/webhook/whatsapp/receive
 *
 * Por que manter o mesmo formato?
 * Porque o backend Java já sabe parsear esse payload (WebhookPayload.java).
 * Assim a migração é transparente — o backend não precisa mudar o controller.
 */

const BACKEND_URL = process.env.BACKEND_WEBHOOK_URL || 'http://localhost:8081/api/webhook/whatsapp/receive';
const BRIDGE_SECRET = process.env.BRIDGE_SECRET || '';

import { getOwnNumber, resolveLid, registerLidMapping } from './connection.js';

/**
 * Processa uma mensagem crua do Baileys e envia pro backend.
 */
export async function handleIncomingMessage(msg, logger) {
    try {
        // Ignora mensagens de status (stories) e reações
        if (msg.key.remoteJid === 'status@broadcast') return;
        if (msg.message?.reactionMessage) return;
        if (msg.message?.protocolMessage) return;

        let remoteJid = msg.key.remoteJid;
        const fromMe = msg.key.fromMe || false;
        const messageId = msg.key.id;

        // ─── RESOLVE LID PARA NÚMERO REAL ─────────────────────────
        // O Baileys em versões recentes pode enviar JIDs no formato @lid
        // (Linked ID) em vez de @s.whatsapp.net. Usamos o mapa de contatos
        // (preenchido pelo evento contacts.upsert) pra resolver.
        let senderPn = null;

        if (remoteJid && remoteJid.endsWith('@lid')) {
            // Tenta resolver pelo mapa LID → número
            const resolvedNumber = resolveLid(remoteJid);

            if (resolvedNumber) {
                senderPn = resolvedNumber;
                remoteJid = resolvedNumber + '@s.whatsapp.net';
            } else if (fromMe) {
                // Mensagem enviada por nós mesmos — usar nosso próprio número
                const own = getOwnNumber();
                if (own) {
                    senderPn = own;
                    remoteJid = own + '@s.whatsapp.net';
                }
            } else {
                // Tenta participant como fallback
                const participant = msg.key.participant;
                if (participant && participant.endsWith('@s.whatsapp.net')) {
                    remoteJid = participant;
                } else if (participant) {
                    const digits = participant.replace(/\D/g, '');
                    if (digits.length >= 10) {
                        senderPn = digits;
                        remoteJid = digits + '@s.whatsapp.net';
                    }
                }
            }

            // Último recurso: passa o LID como "número" pro backend.
            // O Baileys aceita enviar mensagens de volta pra JIDs @lid,
            // então o bot ainda vai conseguir responder esse cliente.
            if (remoteJid.endsWith('@lid')) {
                const lidAsNumber = remoteJid.replace('@lid', '');
                senderPn = lidAsNumber;
                remoteJid = lidAsNumber + '@s.whatsapp.net';
                // Registra pra quando o backend mandar a resposta,
                // o bridge saber que esse "número" é na verdade um LID
                registerLidMapping(lidAsNumber);
                logger.info({ lid: lidAsNumber, messageId }, 'LID não resolvido — usando como identificador direto');
            }
        }

        // Extrai texto da mensagem (vários formatos possíveis no Baileys)
        const text = extractText(msg.message);

        // Detecta tipo de mídia
        const mediaInfo = detectMedia(msg.message);

        logger.info({
            from: remoteJid,
            fromMe,
            messageId,
            hasText: !!text,
            mediaType: mediaInfo?.type || 'none',
        }, 'Mensagem recebida do WhatsApp');

        // Monta o payload no formato que o backend Java espera
        // (mesmo formato da Evolution API — WebhookPayload.java)
        const payload = {
            event: 'messages.upsert',
            data: {
                key: {
                    remoteJid,
                    fromMe,
                    id: messageId,
                    remoteJidAlt: null,
                    senderPn: senderPn,
                },
                message: {
                    // Texto
                    conversation: text,
                    extendedTextMessage: null,
                    // Mídias — o backend só verifica se o campo existe (não null)
                    audioMessage: mediaInfo?.type === 'audio' ? {} : null,
                    imageMessage: mediaInfo?.type === 'image' ? {} : null,
                    videoMessage: mediaInfo?.type === 'video' ? {} : null,
                    documentMessage: mediaInfo?.type === 'document' ? {} : null,
                    stickerMessage: mediaInfo?.type === 'sticker' ? {} : null,
                },
            },
        };

        // Envia pro backend Java
        const response = await fetch(BACKEND_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Bridge-Secret': BRIDGE_SECRET,
            },
            body: JSON.stringify(payload),
            signal: AbortSignal.timeout(10_000), // timeout de 10s
        });

        if (!response.ok) {
            logger.warn({
                status: response.status,
                messageId,
            }, 'Backend retornou erro ao receber mensagem');
        }
    } catch (error) {
        logger.error({
            error: error.message,
            messageId: msg?.key?.id,
        }, 'Falha ao repassar mensagem pro backend');
    }
}

// ─── HELPERS ──────────────────────────────────────────────────

/**
 * Extrai o texto de uma mensagem do Baileys.
 * O Baileys pode mandar o texto em vários campos diferentes
 * dependendo do tipo de mensagem.
 */
function extractText(message) {
    if (!message) return null;

    // Texto simples
    if (message.conversation) return message.conversation;

    // Texto com formatação/link
    if (message.extendedTextMessage?.text) return message.extendedTextMessage.text;

    // Legenda de imagem/vídeo/documento
    if (message.imageMessage?.caption) return message.imageMessage.caption;
    if (message.videoMessage?.caption) return message.videoMessage.caption;
    if (message.documentMessage?.caption) return message.documentMessage.caption;

    // Botões (respostas)
    if (message.buttonsResponseMessage?.selectedDisplayText) {
        return message.buttonsResponseMessage.selectedDisplayText;
    }

    // Lista (respostas)
    if (message.listResponseMessage?.title) {
        return message.listResponseMessage.title;
    }

    return null;
}

/**
 * Detecta se a mensagem contém mídia.
 * Retorna { type: 'audio'|'image'|'video'|'document'|'sticker' } ou null.
 */
function detectMedia(message) {
    if (!message) return null;

    if (message.audioMessage) return { type: 'audio' };
    if (message.imageMessage) return { type: 'image' };
    if (message.videoMessage) return { type: 'video' };
    if (message.documentMessage) return { type: 'document' };
    if (message.stickerMessage) return { type: 'sticker' };

    return null;
}
