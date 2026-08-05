/**
 * index.js — Ponto de entrada do WhatsApp Bridge.
 *
 * Esse arquivo:
 * 1. Carrega variáveis de ambiente (.env)
 * 2. Cria o logger
 * 3. Inicia a conexão com o WhatsApp (Baileys)
 * 4. Registra o handler de mensagens recebidas
 * 5. Sobe o servidor Express (API pro backend Java)
 *
 * Pra rodar:
 *   npm start        (produção)
 *   npm run dev      (desenvolvimento com auto-reload)
 */

// Carrega .env (Node 20+ tem suporte nativo, mas pra Node 18 usamos --env-file ou loadEnv)
import { readFileSync, existsSync } from 'fs';
loadEnvFile();

import pino from 'pino';
import { startConnection, setOnMessageReceived } from './connection.js';
import { handleIncomingMessage } from './message-handler.js';
import { createApi } from './api.js';
import { flushToDisk } from './lid-store.js';

// ─── LOGGER ───────────────────────────────────────────────────

const logger = pino({
    level: process.env.LOG_LEVEL || 'info',
    transport: {
        target: 'pino/file',
        options: { destination: 1 }, // stdout
    },
});

// ─── INICIALIZAÇÃO ────────────────────────────────────────────

async function main() {
    logger.info('=== WhatsApp Bridge iniciando ===');
    logger.info({
        backendUrl: process.env.BACKEND_WEBHOOK_URL,
        port: process.env.BRIDGE_PORT || 3100,
    }, 'Configuração carregada');

    // 1. Conecta no WhatsApp
    await startConnection(logger);

    // 2. Registra o callback de mensagens recebidas
    setOnMessageReceived((msg) => {
        handleIncomingMessage(msg, logger);
    });

    // 3. Sobe o servidor HTTP (API pro backend Java chamar)
    const port = parseInt(process.env.BRIDGE_PORT || '3100', 10);
    const app = createApi(logger);

    app.listen(port, () => {
        logger.info({ port }, 'API do bridge rodando');
        logger.info('Pronto pra receber comandos do backend Java');
    });
}

main().catch((error) => {
    logger.fatal({ error: error.message }, 'Falha fatal ao iniciar o bridge');
    process.exit(1);
});

// ─── GRACEFUL SHUTDOWN ────────────────────────────────────────

process.on('SIGINT', () => {
    logger.info('Encerrando bridge (SIGINT)...');
    flushToDisk();
    process.exit(0);
});

process.on('SIGTERM', () => {
    logger.info('Encerrando bridge (SIGTERM)...');
    flushToDisk();
    process.exit(0);
});

// ─── HELPER: CARREGA .env MANUALMENTE ─────────────────────────
// (Sem dependência extra — funciona em qualquer Node 18+)

function loadEnvFile() {
    const envPath = '.env';
    if (!existsSync(envPath)) return;

    try {
        const content = readFileSync(envPath, 'utf-8');
        for (const line of content.split('\n')) {
            const trimmed = line.trim();
            if (!trimmed || trimmed.startsWith('#')) continue;
            const eqIndex = trimmed.indexOf('=');
            if (eqIndex === -1) continue;
            const key = trimmed.substring(0, eqIndex).trim();
            const value = trimmed.substring(eqIndex + 1).trim();
            // Não sobrescreve variáveis já definidas (prioridade: ambiente > .env)
            if (!process.env[key]) {
                process.env[key] = value;
            }
        }
    } catch {
        // Se não conseguir ler o .env, segue sem ele (variáveis vêm do ambiente)
    }
}
