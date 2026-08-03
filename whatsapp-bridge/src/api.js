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
import { getSocket, getStatus } from './connection.js';

const BRIDGE_SECRET = process.env.BRIDGE_SECRET || '';

export function createApi(logger) {
    const app = express();
    app.use(express.json());

    // ─── MIDDLEWARE DE AUTENTICAÇÃO ───────────────────────────

    app.use((req, res, next) => {
        // Health check não precisa de auth
        if (req.path === '/health') return next();

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
            const jid = formatJid(number);

            // Delay opcional (simula tempo de leitura antes de responder)
            if (delay && delay > 0) {
                await sleep(Math.min(delay, 5000)); // max 5s pra não travar
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

            const jid = formatJid(number);
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

    return app;
}

// ─── HELPERS ──────────────────────────────────────────────────

/**
 * Converte número pra JID do WhatsApp.
 * Entrada: "5534984114981" ou "5534984114981@s.whatsapp.net"
 * Saída: "5534984114981@s.whatsapp.net"
 */
function formatJid(number) {
    // Remove tudo que não é dígito
    const clean = number.replace(/\D/g, '');
    return clean.includes('@') ? number : `${clean}@s.whatsapp.net`;
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
