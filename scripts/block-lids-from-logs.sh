#!/bin/bash
# ────────────────────────────────────────────────────────────────
# block-lids-from-logs.sh
#
# O que faz:
#   Lê os logs do backend, encontra todos os LIDs (números > 13 dígitos)
#   que apareceram no Blocklist check, e bloqueia no Redis via API.
#
# Quando usar:
#   Rode periodicamente ou sempre que quiser garantir que LIDs novos
#   que aparecerem nos logs sejam bloqueados.
#
# Segurança:
#   - Só bloqueia números com mais de 13 dígitos (LIDs)
#   - Números brasileiros reais (55XX...) têm max 13 dígitos, não são afetados
#   - Se bloquear alguém por engano: DELETE /api/webhook/whatsapp/blocklist/{lid}
#
# Uso:
#   ./block-lids-from-logs.sh
#   ou via cron: */30 * * * * /caminho/block-lids-from-logs.sh >> /var/log/blocklist-sync.log 2>&1
# ────────────────────────────────────────────────────────────────

BACKEND_URL="${BACKEND_URL:-http://localhost:8081}"
CONTAINER="${BACKEND_CONTAINER:-toffco-app}"

echo "[$(date)] Iniciando sync de LIDs..."

# Pega LIDs dos logs (números > 13 dígitos que apareceram em Blocklist check)
LIDS=$(docker logs "$CONTAINER" 2>&1 | grep "Blocklist check: number=" | grep -oP 'number=\K[0-9]+' | sort -u)

BLOCKED=0
SKIPPED=0

for NUM in $LIDS; do
    # Só bloqueia se tem mais de 13 dígitos (é LID, não número real)
    if [ ${#NUM} -gt 13 ]; then
        RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BACKEND_URL/api/webhook/whatsapp/blocklist/$NUM")
        if [ "$RESULT" = "200" ]; then
            echo "  Bloqueado LID: $NUM"
            BLOCKED=$((BLOCKED + 1))
        else
            echo "  Falha ao bloquear $NUM (HTTP $RESULT)"
        fi
    else
        SKIPPED=$((SKIPPED + 1))
    fi
done

echo "[$(date)] Concluído: $BLOCKED LIDs bloqueados, $SKIPPED números reais ignorados"
