/**
 * lid-store.js — Mapeamento persistente LID ↔ número real.
 *
 * Salva em disco (JSON) para sobreviver restarts do bridge.
 * Expõe métodos para:
 * - Registrar mapeamentos (vindos de contatos do Baileys)
 * - Resolver LID → número e número → LID
 * - Listar todos os mapeamentos (pro backend consultar)
 *
 * Arquivo de persistência: ./data/lid-mappings.json
 */

import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';

const DATA_DIR = process.env.LID_STORE_PATH || './data';
const FILE_PATH = join(DATA_DIR, 'lid-mappings.json');

// LID (dígitos) → número real (dígitos)
const lidToNumber = new Map();
// Número real (dígitos) → LID (dígitos)
const numberToLid = new Map();

let dirty = false; // Flag para salvar apenas quando há mudanças
let saveTimer = null;

// ─── INICIALIZAÇÃO ────────────────────────────────────────────

export function loadMappings(logger) {
    try {
        if (!existsSync(DATA_DIR)) {
            mkdirSync(DATA_DIR, { recursive: true });
        }

        if (existsSync(FILE_PATH)) {
            const raw = readFileSync(FILE_PATH, 'utf-8');
            const data = JSON.parse(raw);

            if (data && typeof data === 'object') {
                for (const [lid, number] of Object.entries(data)) {
                    if (lid && number) {
                        lidToNumber.set(lid, number);
                        numberToLid.set(number, lid);
                    }
                }
            }

            logger.info(
                { mappings: lidToNumber.size, file: FILE_PATH },
                'LID mappings carregados do disco'
            );
        } else {
            logger.info('Nenhum arquivo de LID mappings encontrado — iniciando vazio');
        }
    } catch (error) {
        logger.warn({ error: error.message }, 'Falha ao carregar LID mappings — iniciando vazio');
    }
}

// ─── REGISTRAR MAPEAMENTO ─────────────────────────────────────

/**
 * Registra um mapeamento LID → número real.
 * Salva automaticamente em disco (com debounce).
 */
export function registerMapping(lid, number, logger) {
    if (!lid || !number) return;

    const cleanLid = lid.replace(/\D/g, '');
    const cleanNumber = number.replace(/\D/g, '');

    if (!cleanLid || !cleanNumber) return;

    // Ignora se o "número" é o próprio LID (sem resolução real)
    if (cleanLid === cleanNumber) return;

    // Ignora se já está mapeado com o mesmo valor
    if (lidToNumber.get(cleanLid) === cleanNumber) return;

    lidToNumber.set(cleanLid, cleanNumber);
    numberToLid.set(cleanNumber, cleanLid);
    dirty = true;

    if (logger) {
        logger.info({ lid: cleanLid, number: cleanNumber }, 'Novo mapeamento LID → número registrado');
    }

    scheduleSave();
}

// ─── RESOLUÇÃO ────────────────────────────────────────────────

/**
 * Resolve um LID para o número real.
 * Retorna null se não conhece.
 */
export function resolveToNumber(lid) {
    if (!lid) return null;
    const clean = lid.replace('@lid', '').replace('@s.whatsapp.net', '').replace(/\D/g, '');
    return lidToNumber.get(clean) || null;
}

/**
 * Resolve um número real para o LID.
 * Retorna null se não conhece.
 */
export function resolveToLid(number) {
    if (!number) return null;
    const clean = number.replace(/\D/g, '');
    return numberToLid.get(clean) || null;
}

/**
 * Verifica se um LID ou número está nos mapeamentos.
 * Retorna o par { lid, number } ou null.
 */
export function findMapping(identifier) {
    if (!identifier) return null;
    const clean = identifier.replace(/\D/g, '');

    // Tenta como LID
    const asNumber = lidToNumber.get(clean);
    if (asNumber) return { lid: clean, number: asNumber };

    // Tenta como número
    const asLid = numberToLid.get(clean);
    if (asLid) return { lid: asLid, number: clean };

    return null;
}

// ─── API ──────────────────────────────────────────────────────

/**
 * Retorna todos os mapeamentos como objeto { lid: number }.
 */
export function getAllMappings() {
    return Object.fromEntries(lidToNumber);
}

/**
 * Retorna estatísticas.
 */
export function getStats() {
    return {
        totalMappings: lidToNumber.size,
        file: FILE_PATH,
    };
}

// ─── PERSISTÊNCIA ─────────────────────────────────────────────

function scheduleSave() {
    if (saveTimer) return; // Já tem um save agendado

    // Debounce de 5 segundos (evita escrita excessiva em disco)
    saveTimer = setTimeout(() => {
        saveTimer = null;
        if (dirty) {
            saveToDisk();
            dirty = false;
        }
    }, 5000);
}

function saveToDisk() {
    try {
        if (!existsSync(DATA_DIR)) {
            mkdirSync(DATA_DIR, { recursive: true });
        }

        const data = Object.fromEntries(lidToNumber);
        writeFileSync(FILE_PATH, JSON.stringify(data, null, 2), 'utf-8');
        console.log(`[lid-store] Mapeamentos salvos em disco: ${lidToNumber.size} entradas`);
    } catch (error) {
        console.error('[lid-store] Falha ao salvar LID mappings:', error.message);
    }
}

/**
 * Força o salvamento imediato (usar no shutdown).
 */
export function flushToDisk() {
    if (dirty) {
        saveToDisk();
        dirty = false;
    }
    if (saveTimer) {
        clearTimeout(saveTimer);
        saveTimer = null;
    }
}
