package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.dto.ChatStatus;
import com.site.toffCo.module.whatzap.monitoring.MessageLogService;
import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotDashboardController {

    private final MessageLogService messageLogService;
    private final WhatsappSessionStore sessionStore;
    private final WhatsappCircuitBreaker circuitBreaker;
    private final WhatsappMonitoringService monitoringService;

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        List<String> messages = messageLogService.getRecentMessages();
        List<WhatsappSession> allSessions = sessionStore.findAll();
        Set<String> blocklist = sessionStore.getBlocklist();
        WhatsappCircuitBreaker.Snapshot circuit = circuitBreaker.snapshot();
        WhatsappMonitoringService.Snapshot metrics = monitoringService.snapshot();

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
                    </div>

                    %s

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

                    <h2>Numeros Bloqueados (%d)</h2>
                    <div class="section">
                        <div class="blocklist">%s</div>
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
                // Sessões em atendimento humano
                humanAttendance > 0 ? "<h2>Em Atendimento Humano</h2><div class='section'><table><thead><tr><th>Numero</th><th>Assunto</th><th>Status</th><th>Tempo</th><th>Ultima Msg</th></tr></thead><tbody>" + humanRows + "</tbody></table></div>" : "",
                // Mensagens
                rows.toString(),
                messages.isEmpty() ? "<p class='empty'>Nenhuma mensagem registrada ainda.</p>" : "",
                // Blocklist
                blocklist.size(),
                blocklist.isEmpty() ? "Nenhum numero bloqueado." : String.join(" | ", blocklist)
        );
    }

    @PostMapping("/dashboard/clear")
    public void clear() {
        messageLogService.clear();
    }

    private String truncate(String text, int max) {
        if (text == null) return "-";
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return safe.length() > max ? safe.substring(0, max) + "..." : safe;
    }
}
