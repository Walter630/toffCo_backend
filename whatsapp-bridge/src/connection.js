/**
 * connection.js — Gerencia a conexão com o WhatsApp via Baileys.
 *
 * O que ele faz:
 * 1. Conecta no WhatsApp usando WebSocket (como se fosse o WhatsApp Web)
 * 2. Na primeira vez, mostra QR code no terminal pra escanear
 * 3. Salva a sessão em disco (pasta auth_data) — depois reconecta sozinho
 * 4. Se cair a conexão, tenta reconectar automaticamente com backoff
 * 5. Exporta o socket (sock) pra outros módulos usarem (enviar msg, typing, etc.)
 */

import {
    makeWASocket,
    useMultiFileAuthState,
    DisconnectReason,
    fetchLatestBaileysVersion,
    makeCacheableSignalKeyStore,
} from '@whiskeysockets/baileys';
import { Boom } from '@hapi/boom';
import qrcode from 'qrcode-terminal';
import { mkdir } from 'fs/promises';
import {
    resolveToNumber,
    resolveToLid,
    registerMapping,
    loadMappings,
    flushToDisk,
    getStats,
} from './lid-store.js';

// ─── ESTADO GLOBAL ────────────────────────────────────────────

let sock = null;
let connectionStatus = 'disconnected'; // disconnected | connecting | open
let ownNumber = null; // Número do WhatsApp conectado (ex: "553488560330")
let qrRetries = 0;
const MAX_QR_RETRIES = 15;

// Mapa LID como número → LID (para envio via @lid)
// Registra LIDs que não conseguimos resolver para número real
// mas sabemos que precisam de @lid para envio.
const unresolvedLids = new Set();

// Callback que será chamado quando uma mensagem chegar.
// Definido por quem importa este módulo (message-handler.js).
let onMessageReceived = null;

// Logger reference (setado em startConnection)
let _logger = null;

// ─── FUNÇÕES PÚBLICAS ─────────────────────────────────────────

export function getSocket() {
    return sock;
}

export function getStatus() {
    return connectionStatus;
}

export function getOwnNumber() {
    return ownNumber;
}

/**
 * Tenta resolver um LID para o número real.
 * Ex: "205192532328666" → "5534984114981"
 */
export function resolveLid(lid) {
    if (!lid) return null;
    return resolveToNumber(lid);
}

/**
 * Dado um "número" (que pode ser um LID usado como identificador),
 * retorna o JID correto pra enviar mensagem.
 * Se o número é um LID conhecido, retorna lid@lid.
 * Senão, retorna numero@s.whatsapp.net.
 */
export function resolveJidForSend(number) {
    if (!number) return null;
    const clean = number.replace(/\D/g, '');

    // Se esse "número" tem um LID associado, envia via @lid
    const lid = resolveToLid(clean);
    if (lid) return lid + '@lid';

    // Se é um LID não resolvido que registramos antes
    if (unresolvedLids.has(clean)) return clean + '@lid';

    return clean + '@s.whatsapp.net';
}

/**
 * Registra que um determinado LID não pôde ser resolvido
 * mas deve ser endereçado via @lid no envio.
 */
export function registerLidMapping(lidDigits) {
    if (!lidDigits) return;
    const clean = lidDigits.replace(/\D/g, '');
    unresolvedLids.add(clean);
    if (_logger) {
        _logger.debug({ lid: clean, unresolvedCount: unresolvedLids.size }, 'LID adicionado à lista de não-resolvidos');
    }
}

/**
 * Registra um mapeamento LID → número real persistente.
 */
export function registerLidToNumber(lid, number) {
    registerMapping(lid, number, _logger);
}

export function setOnMessageReceived(callback) {
    onMessageReceived = callback;
}

// ─── CONEXÃO PRINCIPAL ────────────────────────────────────────

export async function startConnection(logger) {
    _logger = logger;
    const authFolder = process.env.AUTH_FOLDER || './auth_data';

    // Carrega mapeamentos LID persistidos do disco
    loadMappings(logger);

    // Garante que a pasta de auth existe
    await mkdir(authFolder, { recursive: true });

    // Carrega credenciais salvas (ou cria novas na primeira vez)
    const { state, saveCreds } = await useMultiFileAuthState(authFolder);

    // Pega a versão mais recente do protocolo WhatsApp
    const { version } = await fetchLatestBaileysVersion();

    logger.info({ version }, 'Iniciando conexão Baileys');

    // Cria o socket — essa é a conexão real com o WhatsApp
    sock = makeWASocket({
        version,
        auth: {
            creds: state.creds,
            keys: makeCacheableSignalKeyStore(state.keys, logger),
        },
        logger: logger.child({ module: 'baileys' }),
        // Não baixa mídia automaticamente (economiza banda)
        getMessage: async () => undefined,
        // Marca mensagens como recebidas automaticamente (check azul)
        markOnlineOnConnect: true,
        // Gera links de preview? Não — economiza processamento
        generateHighQualityLinkPreview: false,
    });

    // ─── EVENTOS ──────────────────────────────────────────────

    // Quando o status da conexão muda
    sock.ev.on('connection.update', (update) => {
        const { connection, lastDisconnect, qr } = update;

        // QR code pra escanear (só na primeira vez ou quando perder sessão)
        if (qr) {
            qrRetries++;
            if (qrRetries > MAX_QR_RETRIES) {
                logger.error('QR code expirou muitas vezes. Reinicie o bridge.');
                connectionStatus = 'disconnected';
                return;
            }
            logger.info(`QR Code gerado (tentativa ${qrRetries}/${MAX_QR_RETRIES}). Escaneie com o WhatsApp:`);
            qrcode.generate(qr, { small: true });
        }

        if (connection === 'open') {
            connectionStatus = 'open';
            qrRetries = 0;
            // Extrai o número conectado do socket (ex: "553488560330:19@s.whatsapp.net" → "553488560330")
            const meId = sock?.user?.id;
            if (meId) {
                ownNumber = meId.split(':')[0].split('@')[0].replace(/\D/g, '');
            }
            logger.info({ ownNumber }, 'Conectado ao WhatsApp com sucesso!');
        }

        if (connection === 'close') {
            connectionStatus = 'disconnected';
            const reason = new Boom(lastDisconnect?.error)?.output?.statusCode;

            // 401 = deslogado (sessão invalidada) — precisa escanear QR de novo
            if (reason === DisconnectReason.loggedOut) {
                logger.warn('Sessão encerrada pelo WhatsApp. Escaneie o QR code novamente.');
                // Não reconecta automaticamente — precisa de intervenção
                return;
            }

            // Qualquer outro motivo — tenta reconectar
            logger.warn({ reason }, 'Conexão fechada. Reconectando em 3s...');
            setTimeout(() => {
                connectionStatus = 'connecting';
                startConnection(logger);
            }, 3000);
        }

        if (connection === 'connecting') {
            connectionStatus = 'connecting';
        }
    });

    // Salva credenciais sempre que atualizam (rotação de chaves)
    sock.ev.on('creds.update', saveCreds);

    // Quando mensagens chegam
    sock.ev.on('messages.upsert', ({ messages, type }) => {
        // type === 'notify' = mensagem nova em tempo real
        // type === 'append' = histórico sendo carregado (ignoramos)
        if (type !== 'notify') return;

        for (const msg of messages) {
            if (onMessageReceived) {
                onMessageReceived(msg);
            }
        }
    });

    // ─── MAPEAMENTO LID → NÚMERO ─────────────────────────────
    // O Baileys emite contatos com lid e número real.
    // Usamos isso pra resolver JIDs @lid → @s.whatsapp.net.
    // Agora persiste em disco via lid-store.js.

    sock.ev.on('contacts.upsert', (contacts) => {
        for (const contact of contacts) {
            mapContact(contact, logger);
        }
        logger.info({ totalMappings: getMappingCount() }, 'Contatos atualizados no LID store');
    });

    sock.ev.on('contacts.update', (contacts) => {
        for (const contact of contacts) {
            mapContact(contact, logger);
        }
    });

    sock.ev.on('messaging-history.set', ({ contacts }) => {
        if (contacts) {
            for (const contact of contacts) {
                mapContact(contact, logger);
            }
            logger.info({ totalMappings: getMappingCount() }, 'Histórico de contatos carregado no LID store');
        }
    });

    return sock;
}

/**
 * Retorna total de mapeamentos conhecidos.
 */
function getMappingCount() {
    return getStats().totalMappings;
}

/**
 * Extrai o mapeamento LID → número de um contato do Baileys.
 * O Baileys pode mandar: { id: "xxx@lid", lid: "xxx@lid", name: "...", notify: "..." }
 * E em alguns casos: { id: "55xxx@s.whatsapp.net", lid: "xxx@lid" }
 */
function mapContact(contact, logger) {
    if (!contact) return;

    const id = contact.id || '';
    const lid = contact.lid || '';

    // Caso 1: id é @s.whatsapp.net e lid é @lid → mapeia lid → número
    if (id.endsWith('@s.whatsapp.net') && lid.endsWith('@lid')) {
        const number = id.replace('@s.whatsapp.net', '').replace(/\D/g, '');
        const lidClean = lid.replace('@lid', '').replace(/\D/g, '');
        if (number && lidClean) {
            registerMapping(lidClean, number, logger);
            // Se esse LID estava em unresolvedLids, remove
            unresolvedLids.delete(lidClean);
        }
    }

    // Caso 2: id é @lid mas tem verifiedName ou notify com número
    // (raro, mas cobre edge cases)
    if (id.endsWith('@lid') && contact.verifiedName) {
        // verifiedName geralmente é o nome, não o número — ignoramos
    }
}
