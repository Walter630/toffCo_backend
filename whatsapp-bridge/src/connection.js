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
import pino from 'pino';
import { mkdir } from 'fs/promises';

// ─── ESTADO GLOBAL ────────────────────────────────────────────

let sock = null;
let connectionStatus = 'disconnected'; // disconnected | connecting | open
let qrRetries = 0;
const MAX_QR_RETRIES = 15;

// Callback que será chamado quando uma mensagem chegar.
// Definido por quem importa este módulo (message-handler.js).
let onMessageReceived = null;

// ─── FUNÇÕES PÚBLICAS ─────────────────────────────────────────

export function getSocket() {
    return sock;
}

export function getStatus() {
    return connectionStatus;
}

export function setOnMessageReceived(callback) {
    onMessageReceived = callback;
}

// ─── CONEXÃO PRINCIPAL ────────────────────────────────────────

export async function startConnection(logger) {
    const authFolder = process.env.AUTH_FOLDER || './auth_data';

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
            logger.info('Conectado ao WhatsApp com sucesso!');
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

    return sock;
}
