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
const BACKEND_BASE_URL = process.env.BACKEND_BASE_URL || process.env.BACKEND_WEBHOOK_URL?.replace('/api/webhook/whatsapp/receive', '') || 'http://localhost:8081';
const BRIDGE_SECRET = process.env.BRIDGE_SECRET || '';

import { getOwnNumber, getSocket, getStatus, resolveLid, registerLidMapping, registerLidToNumber } from './connection.js';
import { resolveToNumber } from './lid-store.js';

// ─── BLOCKLIST LOCAL (baixada do backend) ─────────────────────
let localBlocklist = new Set();
let blocklistLoaded = false;

/**
 * Baixa a blocklist do backend e mantém em memória.
 * Chamada no startup e periodicamente.
 */
export async function refreshBlocklist(logger) {
    try {
        const response = await fetch(`${BACKEND_BASE_URL}/api/webhook/whatsapp/blocklist`, {
            signal: AbortSignal.timeout(5000),
        });
        if (response.ok) {
            const data = await response.json();
            if (Array.isArray(data)) {
                localBlocklist = new Set(data.map(n => n.replace(/\D/g, '')));
            } else if (data && typeof data === 'object') {
                // Pode vir como Set serializado
                localBlocklist = new Set(Object.values(data).map(n => String(n).replace(/\D/g, '')));
            }
            blocklistLoaded = true;
            if (logger) logger.info({ size: localBlocklist.size }, 'Blocklist baixada do backend');
        } else {
            if (logger) logger.warn({ status: response.status }, 'Falha ao baixar blocklist do backend');
        }
    } catch (error) {
        if (logger) logger.warn({ error: error.message }, 'Erro ao baixar blocklist do backend');
    }
}

/**
 * Força a resolução de TODOS os números da blocklist local para seus LIDs.
 * Usa sock.onWhatsApp() que é SILENCIOSO — ninguém recebe notificação.
 * 
 * Quando resolve, registra o mapeamento no lid-store E notifica o backend
 * chamando /resolve-numbers (que o BlocklistSyncService já consome).
 * 
 * Resultado: o Redis do backend fica com NÚMERO + LID bloqueados.
 */
export async function forceResolveLids(logger) {
    if (!blocklistLoaded || localBlocklist.size === 0) {
        if (logger) logger.debug('forceResolveLids: blocklist vazia, pulando');
        return;
    }

    const { getSocket, getStatus, registerLidToNumber } = await import('./connection.js');
    const { registerMapping } = await import('./lid-store.js');
    const sock = getSocket();
    if (!sock || getStatus() !== 'open') {
        if (logger) logger.warn('forceResolveLids: socket não conectado, pulando');
        return;
    }

    const numbers = [...localBlocklist].filter(n => n.length <= 13); // Só números reais, não LIDs
    if (numbers.length === 0) return;

    if (logger) logger.info({ total: numbers.length }, 'Resolvendo LIDs de todos os números bloqueados...');

    let resolved = 0;

    // Processa em lotes de 5 pra não sobrecarregar
    for (let i = 0; i < numbers.length; i += 5) {
        const batch = numbers.slice(i, i + 5);
        const jids = batch.map(n => n + '@s.whatsapp.net');

        try {
            const results = await Promise.race([
                sock.onWhatsApp(...jids),
                new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 10000))
            ]);

            if (results) {
                for (const result of results) {
                    if (result.exists && result.jid) {
                        const phone = result.jid.replace('@s.whatsapp.net', '').replace(/\D/g, '');

                        // Se o WhatsApp retornou um LID associado
                        if (result.lid) {
                            const lid = result.lid.replace('@lid', '').replace(/\D/g, '');
                            if (lid && lid !== phone) {
                                registerMapping(lid, phone, logger);
                                registerLidToNumber(lid, phone);
                                // Adiciona o LID à blocklist local também
                                localBlocklist.add(lid);
                                resolved++;
                                if (logger) logger.info({ phone, lid }, 'LID resolvido e bloqueado');
                            }
                        }
                    }
                }
            }
        } catch (error) {
            if (logger) logger.debug({ batch, error: error.message }, 'Falha ao resolver lote');
        }

        // Pausa entre lotes pra não ser rate-limited
        if (i + 5 < numbers.length) {
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    if (logger) logger.info({ resolved, total: numbers.length }, 'Resolução de LIDs concluída');
}

/**
 * Verifica se um número/LID está na blocklist local.
 */
function isLocallyBlocked(identifier) {
    if (!blocklistLoaded || !identifier) return false;
    const clean = identifier.replace(/\D/g, '');
    return localBlocklist.has(clean);
}

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
        // ESTRATÉGIA:
        // 1. Se é @lid, tenta resolver para número real via lid-store
        // 2. Se resolve, envia remoteJid = numero@s.whatsapp.net e senderPn = número real
        // 3. Se NÃO resolve, envia remoteJid = lid@s.whatsapp.net (fallback)
        //    mas TAMBÉM envia originalLid para o backend poder checar blocklist
        // 4. Tenta resolução em tempo real via Baileys (assíncrono) para futuras mensagens
        let senderPn = null;
        let originalLid = null;

        if (remoteJid && remoteJid.endsWith('@lid')) {
            originalLid = remoteJid.replace('@lid', '').replace(/\D/g, '');

            // 1. Tenta resolver pelo lid-store persistente
            const resolvedNumber = resolveLid(remoteJid);

            if (resolvedNumber) {
                senderPn = resolvedNumber;
                remoteJid = resolvedNumber + '@s.whatsapp.net';
                logger.debug({ lid: originalLid, resolvedTo: resolvedNumber }, 'LID resolvido pelo store');
            } else {
                // 2. Tenta participant como fallback (mensagens de grupo reencaminhadas)
                const participant = msg.key.participant;
                if (participant && participant.endsWith('@s.whatsapp.net')) {
                    const participantNumber = participant.replace('@s.whatsapp.net', '').replace(/\D/g, '');
                    senderPn = participantNumber;
                    remoteJid = participant;
                    // Registra o mapeamento para futuro
                    registerLidToNumber(originalLid, participantNumber);
                    logger.debug({ lid: originalLid, resolvedTo: participantNumber }, 'LID resolvido via participant');
                } else if (participant) {
                    const digits = participant.replace(/\D/g, '');
                    if (digits.length >= 10 && digits.length <= 15) {
                        senderPn = digits;
                        remoteJid = digits + '@s.whatsapp.net';
                        registerLidToNumber(originalLid, digits);
                    }
                }
            }

            // 3. Último recurso: tenta resolver via sock.onWhatsApp() em TEMPO REAL
            //    antes de decidir se deixa passar ou não.
            if (remoteJid.endsWith('@lid')) {
                const lidAsNumber = remoteJid.replace('@lid', '');
                registerLidMapping(lidAsNumber);

                // Tentativa síncrona: resolve o LID usando o Baileys
                const resolvedRealTime = await tryResolveRealTime(lidAsNumber, logger);
                if (resolvedRealTime) {
                    senderPn = resolvedRealTime;
                    remoteJid = resolvedRealTime + '@s.whatsapp.net';
                    registerLidToNumber(lidAsNumber, resolvedRealTime);
                    logger.info({ lid: lidAsNumber, resolvedTo: resolvedRealTime }, 'LID resolvido em tempo real');
                } else {
                    // NÃO conseguiu resolver. Usa o LID como identificador
                    // mas VERIFICA contra a blocklist do backend antes de repassar.
                    remoteJid = lidAsNumber + '@s.whatsapp.net';
                    logger.info({ lid: lidAsNumber, messageId, fromMe }, 'LID não resolvido — verificando blocklist');
                }
            }
        }

        // Extrai texto da mensagem
        const text = extractText(msg.message);

        // Detecta tipo de mídia
        const mediaInfo = detectMedia(msg.message);

        // ─── BLOCKLIST LOCAL ──────────────────────────────────────
        // Verifica se o número (real ou LID) está bloqueado ANTES
        // de enviar pro backend. Isso é a proteção principal quando
        // o bridge não consegue resolver LID → número real.
        //
        // REGRA EXTRA: Se o LID não foi resolvido (senderPn=null) e
        // o LID tem > 13 dígitos, consulta o backend em tempo real
        // para verificar se deve bloquear.
        if (!fromMe) {
            const numberFromJid = remoteJid.replace('@s.whatsapp.net', '').replace(/\D/g, '');
            const blocked = isLocallyBlocked(numberFromJid)
                || (senderPn && isLocallyBlocked(senderPn))
                || (originalLid && isLocallyBlocked(originalLid));

            if (blocked) {
                logger.info({ number: numberFromJid, senderPn, originalLid }, 'BLOQUEADO pelo bridge (blocklist local)');
                return; // Não repassa pro backend
            }

            // Se é um LID não resolvido (sem número real), consulta o backend
            // para verificação extra. O backend pode ter bloqueado esse LID
            // via dashboard/block-bulk desde o último refresh da blocklist local.
            if (originalLid && !senderPn && numberFromJid.length > 13) {
                const isBlockedOnBackend = await checkBlockedOnBackend(numberFromJid, logger);
                if (isBlockedOnBackend) {
                    // Adiciona à blocklist local pra não consultar de novo
                    localBlocklist.add(numberFromJid);
                    logger.info({ lid: numberFromJid }, 'LID bloqueado após consulta ao backend');
                    return;
                }
            }
        }

        logger.info({
            from: remoteJid,
            fromMe,
            messageId,
            hasText: !!text,
            mediaType: mediaInfo?.type || 'none',
            originalLid: originalLid || undefined,
            senderPn: senderPn || undefined,
        }, 'Mensagem processada');

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
                    // Campo extra: o LID original para o backend checar blocklist
                    originalLid: originalLid,
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

// ─── RESOLUÇÃO EM TEMPO REAL ──────────────────────────────────

/**
 * Tenta resolver um LID para número real em TEMPO REAL usando múltiplas estratégias.
 * Bloqueia o fluxo por no máximo 3 segundos. Se não resolver, retorna null.
 *
 * Estratégias (em ordem):
 * 1. Store de contatos do Baileys (cache local)
 * 2. sock.onWhatsApp() com o LID convertido
 * 3. Consulta ao lid-store persistente (double-check)
 */
async function tryResolveRealTime(lidDigits, logger) {
    try {
        const sock = getSocket();
        if (!sock || getStatus() !== 'open') return null;

        // Estratégia 1: store de contatos
        if (sock.store?.contacts) {
            // Tenta pelo @lid
            const contactByLid = sock.store.contacts[lidDigits + '@lid'];
            if (contactByLid?.id?.endsWith('@s.whatsapp.net')) {
                return contactByLid.id.replace('@s.whatsapp.net', '').replace(/\D/g, '');
            }

            // Busca reversa: procura no store algum contato com o LID
            for (const [jid, contact] of Object.entries(sock.store.contacts)) {
                if (contact.lid === lidDigits + '@lid' && jid.endsWith('@s.whatsapp.net')) {
                    return jid.replace('@s.whatsapp.net', '').replace(/\D/g, '');
                }
            }
        }

        // Estratégia 2: sock.onWhatsApp() — pergunta ao WhatsApp diretamente
        // Isso funciona quando o LID corresponde a um contato no telefone
        try {
            const jidToCheck = lidDigits + '@s.whatsapp.net';
            const results = await Promise.race([
                sock.onWhatsApp(jidToCheck),
                new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 3000))
            ]);

            if (results && results.length > 0) {
                for (const result of results) {
                    if (result.exists && result.jid) {
                        const number = result.jid.replace('@s.whatsapp.net', '').replace(/\D/g, '');
                        if (number && number.length <= 15) {
                            return number;
                        }
                    }
                }
            }
        } catch (e) {
            // Timeout ou erro — segue sem resolver
            logger.debug({ lid: lidDigits, error: e.message }, 'onWhatsApp falhou para LID');
        }

        // Estratégia 3: double-check no lid-store (pode ter sido atualizado por outro processo)
        const fromStore = resolveToNumber(lidDigits);
        if (fromStore) return fromStore;

        return null;
    } catch (error) {
        logger.debug({ lid: lidDigits, error: error.message }, 'Falha na resolução em tempo real');
        return null;
    }
}

/**
 * Consulta o backend em tempo real pra saber se um LID está bloqueado.
 * Usado quando o LID não foi resolvido pra número real.
 */
async function checkBlockedOnBackend(lidNumber, logger) {
    try {
        const response = await fetch(`${BACKEND_BASE_URL}/api/webhook/whatsapp/blocklist`, {
            signal: AbortSignal.timeout(3000),
        });
        if (!response.ok) return false;

        const data = await response.json();
        let blocklist;
        if (Array.isArray(data)) {
            blocklist = new Set(data.map(n => String(n).replace(/\D/g, '')));
        } else if (data && typeof data === 'object') {
            blocklist = new Set(Object.values(data).map(n => String(n).replace(/\D/g, '')));
        } else {
            return false;
        }

        // Atualiza a blocklist local de uma vez (refresh oportunístico)
        localBlocklist = blocklist;
        blocklistLoaded = true;

        return blocklist.has(lidNumber);
    } catch (error) {
        logger.debug({ error: error.message }, 'Falha ao consultar backend para LID');
        return false;
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
