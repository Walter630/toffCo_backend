package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.monitoring.MessageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotDashboardController {

    private final MessageLogService messageLogService;

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard() {
        List<String> messages = messageLogService.getRecentMessages();

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
                    .append("<td>").append(time).append("</td>")
                    .append("<td>").append(direction).append("</td>")
                    .append("<td>").append(number).append("</td>")
                    .append("<td>").append(text).append("</td>")
                    .append("</tr>\n");
        }

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Bot WhatsApp - Monitoramento</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #1a1a2e; color: #eee; padding: 20px; }
                        h1 { margin-bottom: 10px; font-size: 1.4rem; color: #00d4aa; }
                        .info { color: #888; font-size: 0.85rem; margin-bottom: 20px; }
                        .actions { margin-bottom: 15px; display: flex; gap: 10px; }
                        .actions button { padding: 8px 16px; border: none; border-radius: 6px; cursor: pointer; font-size: 0.85rem; }
                        .btn-refresh { background: #00d4aa; color: #1a1a2e; font-weight: 600; }
                        .btn-clear { background: #ff4757; color: #fff; }
                        table { width: 100%%; border-collapse: collapse; font-size: 0.85rem; }
                        th { background: #16213e; padding: 10px 8px; text-align: left; position: sticky; top: 0; }
                        td { padding: 8px; border-bottom: 1px solid #2a2a4a; word-break: break-word; max-width: 400px; }
                        tr.received td:nth-child(2) { color: #74b9ff; }
                        tr.sent td:nth-child(2) { color: #00d4aa; }
                        tr.event td:nth-child(2) { color: #ffa502; }
                        tr:hover { background: #16213e; }
                        .empty { text-align: center; padding: 40px; color: #666; }
                        @media (max-width: 600px) {
                            td, th { padding: 6px 4px; font-size: 0.75rem; }
                            td { max-width: 200px; }
                        }
                    </style>
                </head>
                <body>
                    <h1>Bot WhatsApp - Monitoramento</h1>
                    <p class="info">Ultimas %d mensagens. Atualiza automaticamente a cada 5s.</p>
                    <div class="actions">
                        <button class="btn-refresh" onclick="location.reload()">Atualizar</button>
                        <button class="btn-clear" onclick="limpar()">Limpar Log</button>
                    </div>
                    <table>
                        <thead><tr><th>Hora</th><th>Tipo</th><th>Numero</th><th>Mensagem</th></tr></thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    %s
                    <script>
                        setTimeout(() => location.reload(), 5000);
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
                rows.toString(),
                messages.isEmpty() ? "<p class='empty'>Nenhuma mensagem registrada ainda.</p>" : ""
        );
    }

    @PostMapping("/dashboard/clear")
    public void clear() {
        messageLogService.clear();
    }
}
