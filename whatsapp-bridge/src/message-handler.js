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
 * REGRA FUNDAMENTAL:
 * - remoteJid SEMPRE representa o OUTRO LADO da conversa (o cliente).
 * - Quando fromMe=false → remoteJid = quem mandou a mensagem (cliente)
 * - Quando fromMe=true  → remoteJid = pra quem o gerente mandou (cliente)
 * - Em ambos os casos, o backend precisa receber o NÚMERO DO CLIENTE.
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

        // Ignora mensagens de grupo
        if (remoteJid && remoteJid.endsWith('@g.us')) return;

        // ─── RESOLVE LID PARA NÚMERO/IDENTIFICADOR ────────────────
        //
        // O remoteJid SEMPRE representa o CLIENTE (o outro lado):
        // - fromMe=false → cliente mandou mensagem pro gerente
        // - fromMe=true  → gerente mandou mensagem pro cliente
        //
        // Em ambos os casos, precisamos resolver o LID do CLIENTE.
        let senderPn = null;

        if (remoteJid && remoteJid.endsWith('@lid')) {
            // 1. Tenta resolver pelo mapa de contatos
            const resolvedNumber = resolveLid(remoteJid);

            if (resolvedNumber) {
                senderPn = resolvedNumber;
                remoteJid = resolvedNumber + '@s.whatsapp.net';
            } else {
                // 2. Tenta participant como fallback
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

            // 3. Último recurso: usa o LID como identificador
            if (remoteJid.endsWith('@lid')) {
                const lidAsNumber = remoteJid.replace('@lid', '');
                senderPn = lidAsNumber;
                remoteJid = lidAsNumber + '@s.whatsapp.net';
                registerLidMapping(lidAsNumber);
                logger.debug({ lid: lidAsNumber, messageId, fromMe }, 'LID usado como identificador direto');
            }
        }

        // Extrai texto da mensagem
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
                    conversation: text,
                    extendedTextMessage: null,
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
            signal: AbortSignal.timeout(10_000),
        });

        if (!response.ok) {
            logger.warn({ status: response.status, messageId }, 'Backend retornou erro');
        }
    } catch (error) {
        logger.error({ error: error.message, messageId: msg?.key?.id }, 'Falha ao repassar mensagem pro backend');
    }
}

// ─── HELPERS ──────────────────────────────────────────────────

function extractText(message) {
    if (!message) return null;
    if (message.conversation) return message.conversation;
    if (message.extendedTextMessage?.text) return message.extendedTextMessage.text;
    if (message.imageMessage?.caption) return message.imageMessage.caption;
    if (message.videoMessage?.caption) return message.videoMessage.caption;
    if (message.documentMessage?.caption) return message.documentMessage.caption;
    if (message.buttonsResponseMessage?.selectedDisplayText) return message.buttonsResponseMessage.selectedDisplayText;
    if (message.listResponseMessage?.title) return message.listResponseMessage.title;
    return null;
}

function detectMedia(message) {
    if (!message) return null;
    if (message.audioMessage) return { type: 'audio' };
    if (message.imageMessage) return { type: 'image' };
    if (message.videoMessage) return { type: 'video' };
    if (message.documentMessage) return { type: 'document' };
    if (message.stickerMessage) return { type: 'sticker' };
    return null;
}
