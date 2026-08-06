package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.dto.ChatStatus;
import com.site.toffCo.module.whatzap.monitoring.MessageLogService;
import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import com.site.toffCo.module.whatzap.service.BlocklistSyncService;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotDashboardController {

    private final MessageLogService messageLogService;
    private final WhatsappSessionStore sessionStore;
    private final WhatsappCircuitBreaker circuitBreaker;
    private final WhatsappMonitoringService monitoringService;
    private final BlocklistSyncService blocklistSyncService;
    private final WhatsappProperties whatsappProperties;

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        List<String> messages = messageLogService.getRecentMessages();
        List<WhatsappSession> allSessions = sessionStore.findAll();
        Set<String> blocklist = sessionStore.getBlocklist();
        WhatsappCircuitBreaker.Snapshot circuit = circuitBreaker.snapshot();
        WhatsappMonitoringService.Snapshot metrics = monitoringService.snapshot();

        // LIDs pendentes de bloqueio
        Set<String> unblockedLids = blocklistSyncService.getUnblockedSeenLids();
        List<String> configuredNumbers = whatsappProperties.blockedNumbers();

        // Sessões ativas
        long activeSessions = allSessions.size();
        long humanAttendance = allSessions.stream()
                .filter(WhatsappSession::isHumanAssigned)
                .count();
        long pendingQueue = allSessions.stream()
                .filter(s -> s.getStatus() == ChatStatus.PENDING)
                .count();

        // Tabela de mensagens
        StringBuilder rows = new StringBuilder();
        for (String msg : messages) {
            String[] parts = msg.split("\\|", 4);
            if (parts.length < 4) {
                rows.append("<tr><td colspan='4'>").append(msg).append("</td></tr>\n");
                continue;
            }
            String time = parts[0].trim();
            String direction = parts[1].trim();
            String number = parts[2].trim();
            String text = parts[3].trim();

            String rowClass = direction.contains("RECEBIDA") ? "received"
                    : direction.contains("ENVIADA") ? "sent"
                    : "event";

            rows.append("<tr class='").append(rowClass).append("'>")
                    .append("<td class='time'>").append(time).append("</td>")
                    .append("<td class='dir'>").append(direction).append("</td>")
                    .append("<td class='num'>").append(number).append("</td>")
                    .append("<td class='msg'>").append(text).append("</td>")
                    .append("</tr>\n");
        }

        // Tabela de sessões em atendimento humano
        StringBuilder humanRows = new StringBuilder();
        allSessions.stream()
                .filter(WhatsappSession::isHumanAssigned)
                .forEach(s -> {
                    long mins = s.getHumanAssingnedAt() != null
                            ? Duration.between(s.getHumanAssingnedAt(), Instant.now()).toMinutes()
                            : 0;
                    humanRows.append("<tr>")
                            .append("<td>").append(s.getWhatsappId()).append("</td>")
                            .append("<td>").append(s.getAttendanceSubject() != null ? s.getAttendanceSubject() : "-").append("</td>")
                            .append("<td>").append(s.getStatus() != null ? s.getStatus().name() : "-").append("</td>")
                            .append("<td>").append(mins).append(" min</td>")
                            .append("<td>").append(s.getLastMessage() != null ? truncate(s.getLastMessage(), 50) : "-").append("</td>")
                            .append("</tr>\n");
                });

        // Seção de LIDs pendentes
        StringBuilder lidRows = new StringBuilder();
        for (String lid : unblockedLids) {
            lidRows.append("<tr>")
                    .append("<td class='num'>").append(lid).append("</td>")
                    .append("<td><button class='btn-block' onclick=\"bloquearLid('").append(lid).append("')\">Bloquear</button></td>")
                    .append("</tr>\n");
        }

        // Números configurados (da .env) - mostra quais já estão no Redis e quais não
        StringBuilder configRows = new StringBuilder();
        for (String number : configuredNumbers) {
            boolean inRedis = sessionStore.isBlocked(number);
            String status = inRedis ? "<span class='ok-text'>Bloqueado</span>" : "<span class='warn-text'>Pendente</span>";
            configRows.append("<tr>")
                    .append("<td class='num'>").append(number).append("</td>")
                    .append("<td>").append(status).append("</td>")
                    .append("</tr>\n");
        }

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Bot WhatsApp - Painel</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f0f23; color: #eee; padding: 16px; }
                        h1 { font-size: 1.3rem; color: #00d4aa; margin-bottom: 4px; }
                        h2 { font-size: 1rem; color: #74b9ff; margin: 20px 0 8px; }
                        .subtitle { color: #666; font-size: 0.8rem; margin-bottom: 16px; }

                        .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 20px; }
                        .card { background: #1a1a3e; border-radius: 8px; padding: 12px; text-align: center; }
                        .card .value { font-size: 1.6rem; font-weight: 700; color: #00d4aa; }
                        .card .label { font-size: 0.7rem; color: #888; margin-top: 4px; }
                        .card.warn .value { color: #ffa502; }
                        .card.danger .value { color: #ff4757; }

                        .actions { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
                        .actions button { padding: 6px 14px; border: none; border-radius: 6px; cursor: pointer; font-size: 0.8rem; }
                        .btn-refresh { background: #00d4aa; color: #0f0f23; font-weight: 600; }
                        .btn-clear { background: #ff4757; color: #fff; }
                        .btn-pause { background: #ffa502; color: #0f0f23; font-weight: 600; }
                        .btn-block { background: #ff4757; color: #fff; border: none; padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 0.72rem; }
                        .btn-block:hover { background: #ee3344; }
                        .btn-block-all { background: #ff6348; color: #fff; border: none; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 0.8rem; font-weight: 600; }
                        .btn-block-all:hover { background: #ff4757; }

                        table { width: 100%%; border-collapse: collapse; font-size: 0.78rem; margin-bottom: 20px; }
                        th { background: #1a1a3e; padding: 8px 6px; text-align: left; position: sticky; top: 0; z-index: 1; }
                        td { padding: 6px; border-bottom: 1px solid #1a1a3e; }
                        .time { white-space: nowrap; color: #888; width: 90px; }
                        .dir { white-space: nowrap; width: 100px; }
                        .num { white-space: nowrap; font-family: monospace; font-size: 0.72rem; width: 130px; }
                        .msg { word-break: break-word; max-width: 450px; }
                        tr.received .dir { color: #74b9ff; }
                        tr.sent .dir { color: #00d4aa; }
                        tr.event .dir { color: #ffa502; }
                        tr:hover { background: #1a1a3e; }

                        .section { background: #12122a; border-radius: 8px; padding: 14px; margin-bottom: 16px; }
                        .blocklist { font-family: monospace; font-size: 0.75rem; color: #888; max-height: 80px; overflow-y: auto; }
                        .empty { text-align: center; padding: 20px; color: #444; font-size: 0.85rem; }
                        .ok-text { color: #00d4aa; }
                        .warn-text { color: #ffa502; }
                        .lid-alert { color: #ff4757; font-weight: 600; }

                        .tab-nav { display: flex; gap: 4px; margin-bottom: 12px; }
                        .tab-nav button { padding: 8px 16px; border: 1px solid #333; border-radius: 6px 6px 0 0; background: #1a1a3e; color: #888; cursor: pointer; font-size: 0.78rem; border-bottom: none; }
                        .tab-nav button.active { background: #12122a; color: #eee; border-color: #74b9ff; }
                        .tab-content { display: none; }
                        .tab-content.active { display: block; }

                        @media (max-width: 700px) {
                            .cards { grid-template-columns: repeat(2, 1fr); }
                            td, th { padding: 4px 3px; font-size: 0.7rem; }
                            .msg { max-width: 180px; }
                            .num { width: auto; }
                        }
                    </style>
                </head>
                <body>
                    <h1>Bot WhatsApp - Painel de Monitoramento</h1>
                    <p class="subtitle">Atualiza a cada 5s | %d mensagens no log | %s</p>

                    <div class="cards">
                        <div class="card"><div class="value">%d</div><div class="label">Sessoes Ativas</div></div>
                        <div class="card %s"><div class="value">%d</div><div class="label">Atendimento Humano</div></div>
                        <div class="card %s"><div class="value">%d</div><div class="label">Fila Pendente</div></div>
                        <div class="card"><div class="value">%d</div><div class="label">Msgs Enviadas</div></div>
                        <div class="card"><div class="value">%d ms</div><div class="label">Latencia Media</div></div>
                        <div class="card %s"><div class="value">%s</div><div class="label">Circuit Breaker</div></div>
                        <div class="card"><div class="value">%d</div><div class="label">Bloqueados</div></div>
                        <div class="card %s"><div class="value">%d</div><div class="label">LIDs Pendentes</div></div>
                    </div>

                    %s

                    <div class="tab-nav">
                        <button class="active" onclick="showTab('messages')">Mensagens</button>
                        <button onclick="showTab('lids')">LIDs Pendentes %s</button>
                        <button onclick="showTab('blocklist')">Bloqueados</button>
                    </div>

                    <div id="tab-messages" class="tab-content active">
                        <h2>Mensagens Recentes</h2>
                        <div class="actions">
                            <button class="btn-refresh" onclick="location.reload()">Atualizar</button>
                            <button class="btn-pause" id="pauseBtn" onclick="togglePause()">Pausar</button>
                            <button class="btn-clear" onclick="limpar()">Limpar Log</button>
                        </div>
                        <div class="section">
                            <table>
                                <thead><tr><th>Hora</th><th>Tipo</th><th>Numero</th><th>Mensagem</th></tr></thead>
                                <tbody>%s</tbody>
                            </table>
                            %s
                        </div>
                    </div>

                    <div id="tab-lids" class="tab-content">
                        <h2>LIDs Pendentes de Bloqueio (%d)</h2>
                        <p style="color:#888;font-size:0.78rem;margin-bottom:12px;">
                            LIDs sao identificadores alternativos que o WhatsApp usa internamente. Numeros com mais de 13 digitos
                            sao LIDs e podem burlar o bloqueio por numero real. Aqui voce ve quais LIDs foram detectados mas ainda
                            nao estao bloqueados.
                        </p>
                        %s
                        <div class="section">
                            %s
                        </div>

                        <h2>Numeros Configurados (.env) - Status</h2>
                        <p style="color:#888;font-size:0.78rem;margin-bottom:12px;">
                            Comparacao dos numeros reais configurados no .env com o Redis. Se aparecer "Pendente", o numero pode
                            nao estar efetivamente bloqueado.
                        </p>
                        <div class="section">
                            <table>
                                <thead><tr><th>Numero Real</th><th>Status no Redis</th></tr></thead>
                                <tbody>%s</tbody>
                            </table>
                            %s
                        </div>
                    </div>

                    <div id="tab-blocklist" class="tab-content">
                        <h2>Numeros Bloqueados (%d)</h2>
                        <div class="section">
                            <div class="blocklist">%s</div>
                        </div>
                    </div>

                    <script>
                        let paused = false;
                        function togglePause() {
                            paused = !paused;
                            document.getElementById('pauseBtn').textContent = paused ? 'Retomar' : 'Pausar';
                        }
                        if (!paused) setTimeout(() => location.reload(), 5000);
                        function limpar() {
                            if (confirm('Limpar todo o log?')) {
                                fetch('/api/bot/dashboard/clear', { method: 'POST' }).then(() => location.reload());
                            }
                        }

                        function showTab(name) {
                            document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
                            document.querySelectorAll('.tab-nav button').forEach(el => el.classList.remove('active'));
                            document.getElementById('tab-' + name).classList.add('active');
                            event.target.classList.add('active');
                        }

                        function bloquearLid(lid) {
                            if (!confirm('Bloquear LID ' + lid + '?')) return;
                            fetch('/api/bot/dashboard/block-lid/' + lid, { method: 'POST' })
                                .then(r => r.json())
                                .then(data => {
                                    if (data.blocked) {
                                        alert('LID bloqueado com sucesso!');
                                        location.reload();
                                    } else {
                                        alert('Erro ao bloquear: ' + (data.error || 'desconhecido'));
                                    }
                                })
                                .catch(() => alert('Erro de conexao'));
                        }

                        function bloquearTodosLids() {
                            if (!confirm('Bloquear TODOS os LIDs pendentes? Isso pode bloquear clientes legitimos que usam LID.')) return;
                            fetch('/api/bot/dashboard/block-all-lids', { method: 'POST' })
                                .then(r => r.json())
                                .then(data => {
                                    alert(data.blocked + ' LIDs bloqueados!');
                                    location.reload();
                                })
                                .catch(() => alert('Erro de conexao'));
                        }
                    </script>
                </body>
                </html>
                """.formatted(
                messages.size(),
                circuit.state().name(),
                // Cards
                activeSessions,
                humanAttendance > 0 ? "warn" : "", humanAttendance,
                pendingQueue > 0 ? "warn" : "", pendingQueue,
                metrics.successes(),
                metrics.averageLatencyMs(),
                circuit.state() == WhatsappCircuitBreaker.State.OPEN ? "danger" : "", circuit.state().name(),
                blocklist.size(),
                unblockedLids.isEmpty() ? "" : "danger", unblockedLids.size(),
                // Sessões em atendimento humano
                humanAttendance > 0 ? "<h2>Em Atendimento Humano</h2><div class='section'><table><thead><tr><th>Numero</th><th>Assunto</th><th>Status</th><th>Tempo</th><th>Ultima Msg</th></tr></thead><tbody>" + humanRows + "</tbody></table></div>" : "",
                // Tab badge para LIDs
                unblockedLids.isEmpty() ? "" : "<span class='lid-alert'>(" + unblockedLids.size() + ")</span>",
                // Mensagens
                rows.toString(),
                messages.isEmpty() ? "<p class='empty'>Nenhuma mensagem registrada ainda.</p>" : "",
                // Tab LIDs
                unblockedLids.size(),
                unblockedLids.isEmpty() ? "" : "<div class='actions'><button class='btn-block-all' onclick='bloquearTodosLids()'>Bloquear Todos (" + unblockedLids.size() + ")</button></div>",
                unblockedLids.isEmpty()
                        ? "<p class='empty'>Nenhum LID pendente de bloqueio. Tudo certo!</p>"
                        : "<table><thead><tr><th>LID</th><th>Acao</th></tr></thead><tbody>" + lidRows + "</tbody></table>",
                // Números configurados
                configRows.toString(),
                configuredNumbers.isEmpty() ? "<p class='empty'>Nenhum numero configurado no .env.</p>" : "",
                // Blocklist
                blocklist.size(),
                blocklist.isEmpty() ? "Nenhum numero bloqueado." : String.join(" | ", blocklist)
        );
    }

    // ─── ENDPOINTS DO DASHBOARD ─────────────────────────────────

    @PostMapping("/dashboard/clear")
    public void clear() {
        messageLogService.clear();
    }

    @PostMapping("/dashboard/block-lid/{lid}")
    public ResponseEntity<Map<String, Object>> blockLid(@PathVariable String lid) {
        if (lid == null || lid.isBlank() || lid.length() <= 13) {
            return ResponseEntity.badRequest().body(Map.of(
                    "blocked", false,
                    "error", "LID invalido (deve ter mais de 13 digitos)"
            ));
        }

        sessionStore.blockNumber(lid);
        log.info("LID bloqueado via painel: {}", lid);
        return ResponseEntity.ok(Map.of("blocked", true, "lid", lid));
    }

    @PostMapping("/dashboard/block-all-lids")
    public ResponseEntity<Map<String, Object>> blockAllLids() {
        Set<String> lids = blocklistSyncService.getUnblockedSeenLids();
        int count = 0;
        for (String lid : lids) {
            sessionStore.blockNumber(lid);
            count++;
            log.info("LID bloqueado via painel (lote): {}", lid);
        }
        return ResponseEntity.ok(Map.of("blocked", count));
    }

    /**
     * Bloqueio em lote: aceita uma lista de números e/ou LIDs e bloqueia todos
     * no Redis de uma vez. O bot não responde nada — apenas registra o bloqueio.
     *
     * Body esperado (JSON):
     * { "numbers": ["5534984114981", "5534988560330", "123456789012345@lid"] }
     *
     * Aceita qualquer formato: número real, LID, com ou sem @lid/@s.whatsapp.net.
     */
    @PostMapping("/dashboard/block-bulk")
    public ResponseEntity<Map<String, Object>> blockBulk(@RequestBody Map<String, List<String>> body) {
        List<String> numbers = body.get("numbers");

        if (numbers == null || numbers.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "blocked", 0,
                    "error", "Campo 'numbers' vazio ou ausente"
            ));
        }

        int count = 0;
        for (String raw : numbers) {
            if (raw == null || raw.isBlank()) continue;

            // Remove sufixos do WhatsApp (@s.whatsapp.net, @lid, @g.us)
            String cleaned = raw.trim()
                    .replace("@s.whatsapp.net", "")
                    .replace("@lid", "")
                    .replace("@g.us", "")
                    .replaceAll("\\D", "");

            if (cleaned.isBlank()) continue;

            if (!sessionStore.isBlocked(cleaned)) {
                sessionStore.blockNumber(cleaned);
                log.info("Número bloqueado via bulk: {}", cleaned);
                count++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "blocked", count,
                "total_received", numbers.size()
        ));
    }

    // ─── HELPERS ────────────────────────────────────────────────

    private String truncate(String text, int max) {
        if (text == null) return "-";
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return safe.length() > max ? safe.substring(0, max) + "..." : safe;
    }
}
