/**
 * api.js — Server Express que o backend Java chama pra enviar mensagens.
 *
 * Endpoints:
 *
 * POST /send-message     → Envia texto pro WhatsApp
 * POST /send-presence    → Mostra "digitando..." pro cliente
 * GET  /health           → Status da conexão (pra monitoramento)
 *
 * O backend Java chama esses endpoints em vez da Evolution API.
 * Exemplo: em vez de POST evolution.api/message/sendText/Bot_toffBrasil
 *          agora faz POST localhost:3100/send-message
 */

import express from 'express';
import { getSocket, getStatus, resolveJidForSend } from './connection.js';
import { getAllMappings, getStats, resolveToNumber, resolveToLid, registerMapping } from './lid-store.js';

const BRIDGE_SECRET = process.env.BRIDGE_SECRET || '';

export function createApi(logger) {
    const app = express();
    app.use(express.json());

    // ─── MIDDLEWARE DE AUTENTICAÇÃO ───────────────────────────

    app.use((req, res, next) => {
        // Endpoints read-only e internos não precisam de auth
        if (req.path === '/health') return next();
        if (req.path === '/lid-mappings') return next();
        if (req.path.startsWith('/lid-mappings/')) return next();
        if (req.path === '/resolve-lid') return next();
        if (req.path === '/resolve-numbers') return next();

        const secret = req.headers['x-bridge-secret'];
        if (BRIDGE_SECRET && secret !== BRIDGE_SECRET) {
            logger.warn({ path: req.path, ip: req.ip }, 'Requisição sem autenticação válida');
            return res.status(401).json({ error: 'Não autorizado' });
        }
        next();
    });

    // ─── POST /send-message ───────────────────────────────────
    //
    // Body esperado (mesmo que o backend já manda pro Evolution):
    // {
    //   "number": "5534984114981",
    //   "text": "Olá! Bem-vindo à ToffCo...",
    //   "delay": 2200  (opcional — espera antes de enviar, simula leitura)
    // }

    app.post('/send-message', async (req, res) => {
        try {
            const { number, text, delay } = req.body;

            if (!number || !text) {
                return res.status(400).json({ error: 'Campos "number" e "text" são obrigatórios' });
            }

            const sock = getSocket();
            if (!sock || getStatus() !== 'open') {
                logger.warn('Tentativa de envio com conexão fechada');
                return res.status(503).json({ error: 'WhatsApp desconectado' });
            }

            // Formata o JID (formato que o Baileys espera)
            // Se o número é um LID registrado, envia pra @lid direto
            const jid = resolveJidForSend(number);

            // Delay opcional (simula tempo de leitura antes de responder)
            if (delay && delay > 0) {
                await sleep(Math.min(delay, 5000)); // max 5s pra não travar
            }
            const urlMatch = text.match(/https?:\/\/[^\s]+/);
            let messageContent;

            if (urlMatch) {
                messageContent = {
                    text,
                    contextInfo: {
                        externalAdReply: {
                            title: 'ToffBrasil',
                            body: 'ToffBr — catálogo de impressões 3D e filamentos',
                            thumbnailUrl: 'https://toffbr.com.br/icon-192x192.png',
                            sourceUrl: urlMatch[0],
                            mediaType: 1,
                            renderLargerThumbnail: true
                        }
                    }
                };
            } else {
                messageContent = { text };
            }
            // Envia a mensagem
            const result = await sock.sendMessage(jid, { text });

            logger.info({ number, messageId: result?.key?.id }, 'Mensagem enviada');

            return res.json({
                success: true,
                messageId: result?.key?.id,
            });
        } catch (error) {
            logger.error({ error: error.message, number: req.body?.number }, 'Falha ao enviar mensagem');
            return res.status(500).json({ error: 'Falha ao enviar: ' + error.message });
        }
    });

    // ─── POST /send-presence ──────────────────────────────────
    //
    // Mostra "digitando..." no chat do cliente.
    // Body: { "number": "5534984114981", "presence": "composing" }
    // presence pode ser: "composing" (digitando) ou "recording" (gravando áudio)

    app.post('/send-presence', async (req, res) => {
        try {
            const { number, presence } = req.body;

            if (!number) {
                return res.status(400).json({ error: 'Campo "number" é obrigatório' });
            }

            const sock = getSocket();
            if (!sock || getStatus() !== 'open') {
                return res.status(503).json({ error: 'WhatsApp desconectado' });
            }

            const jid = resolveJidForSend(number);
            const presenceType = presence || 'composing';

            await sock.presenceSubscribe(jid);
            await sock.sendPresenceUpdate(presenceType, jid);

            return res.json({ success: true });
        } catch (error) {
            // Presence não é crítico — se falhar, não é problema
            logger.debug({ error: error.message }, 'Falha ao enviar presença (não crítico)');
            return res.json({ success: true });
        }
    });

    // ─── GET /health ──────────────────────────────────────────
    //
    // Retorna status da conexão. Útil pra monitoramento e Docker healthcheck.

    app.get('/health', (req, res) => {
        const status = getStatus();
        const statusCode = status === 'open' ? 200 : 503;

        return res.status(statusCode).json({
            status,
            uptime: process.uptime(),
            timestamp: new Date().toISOString(),
        });
    });

    // ─── POST /resolve-numbers ────────────────────────────────
    app.post('/resolve-numbers', async (req, res) => {
        try {
            const { numbers } = req.body;
            if (!numbers || !Array.isArray(numbers)) {
                return res.status(400).json({ error: 'Campo "numbers" (array) obrigatório' });
            }
            const sock = getSocket();
            if (!sock || getStatus() !== 'open') {
                return res.status(503).json({ error: 'WhatsApp desconectado' });
            }
            const mappings = {};

            // Primeiro: verifica os que já temos no lid-store
            for (const number of numbers) {
                const clean = number.replace(/\D/g, '');
                const lid = resolveToLid(clean);
                if (lid) {
                    mappings[clean] = lid;
                }
            }

            // Depois: tenta resolver os restantes via Baileys
            const unresolved = numbers.filter(n => !mappings[n.replace(/\D/g, '')]);

            for (let i = 0; i < unresolved.length; i += 5) {
                const batch = unresolved.slice(i, i + 5);
                const jids = batch.map(n => n.replace(/\D/g, '') + '@s.whatsapp.net');
                try {
                    const results = await sock.onWhatsApp(...jids);
                    for (const result of results) {
                        if (result.exists) {
                            const phone = result.jid.replace('@s.whatsapp.net', '').replace(/\D/g, '');
                            if (result.lid) {
                                const lid = result.lid.replace('@lid', '');
                                mappings[phone] = lid;
                            }
                        }
                    }
                } catch (batchError) {
                    logger.warn({ error: batchError.message }, 'Erro ao resolver lote');
                }
                if (i + 5 < unresolved.length) await sleep(300);
            }
            logger.info({ resolved: Object.keys(mappings).length, total: numbers.length }, 'Números resolvidos');
            return res.json({ mappings });
        } catch (error) {
            logger.error({ error: error.message }, 'Falha ao resolver números');
            return res.status(500).json({ error: error.message });
        }
    });

    // ─── GET /lid-mappings ────────────────────────────────────
    //
    // Retorna todos os mapeamentos LID↔número conhecidos.
    // O backend Java pode consultar pra enriquecer a blocklist.

    app.get('/lid-mappings', (req, res) => {
        return res.json({
            mappings: getAllMappings(),
            stats: getStats(),
        });
    });

    // ─── GET /contacts-dump ───────────────────────────────────
    //
    // Despeja TODOS os contatos que o Baileys conhece.
    // Útil pra debug: ver quais contatos têm LID mapeado.

    app.get('/contacts-dump', (req, res) => {
        const sock = getSocket();
        if (!sock) {
            return res.status(503).json({ error: 'Socket não disponível' });
        }

        // O Baileys mantém contatos em sock.store?.contacts ou sock.authState
        // Nas versões mais novas, os contatos são emitidos via eventos
        // e armazenados no nosso lid-store.
        return res.json({
            lidStoreMappings: getAllMappings(),
            stats: getStats(),
            note: 'Use /resolve-numbers com POST para forçar resolução de números específicos'
        });
    });

    // ─── POST /lid-mappings/import ────────────────────────────
    //
    // Importa mapeamentos LID→número manualmente.
    // Body: { "116964861181994": "5534984114981", ... }
    // Útil quando você sabe qual LID corresponde a qual número.

    app.post('/lid-mappings/import', (req, res) => {
        const mappings = req.body;
        if (!mappings || typeof mappings !== 'object') {
            return res.status(400).json({ error: 'Body deve ser objeto { lid: numero }' });
        }

        let count = 0;
        for (const [lid, number] of Object.entries(mappings)) {
            if (lid && number) {
                registerMapping(lid, number, logger);
                count++;
            }
        }

        logger.info({ imported: count }, 'Mapeamentos LID importados manualmente');
        return res.json({ imported: count, total: getStats().totalMappings });
    });

    // ─── POST /resolve-lid ────────────────────────────────────
    //
    // Dado um LID, retorna o número real (se conhecido).
    // Body: { "lid": "116964861181994" }

    app.post('/resolve-lid', (req, res) => {
        const { lid } = req.body;
        if (!lid) {
            return res.status(400).json({ error: 'Campo "lid" obrigatório' });
        }
        const number = resolveToNumber(lid);
        return res.json({ lid, number: number || null, resolved: !!number });
    });

    return app;
}

// ─── HELPERS ──────────────────────────────────────────────────

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
