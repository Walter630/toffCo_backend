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

/**
 * Processa uma mensagem crua do Baileys e envia pro backend.
 */
export async function handleIncomingMessage(msg, logger) {
    try {
        // Ignora mensagens de status (stories) e reações
        if (msg.key.remoteJid === 'status@broadcast') return;
        if (msg.message?.reactionMessage) return;
        if (msg.message?.protocolMessage) return;

        const remoteJid = msg.key.remoteJid;
        const fromMe = msg.key.fromMe || false;
        const messageId = msg.key.id;

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
                    // Baileys já resolve o JID real — não precisa de remoteJidAlt
                    remoteJidAlt: null,
                    senderPn: null,
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
