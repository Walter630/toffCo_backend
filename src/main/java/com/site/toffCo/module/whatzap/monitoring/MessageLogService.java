package com.site.toffCo.module.whatzap.monitoring;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Armazena as últimas mensagens trocadas pelo bot no Redis (lista limitada).
 * Usado pelo painel de monitoramento HTML.
 */
@Service
public class MessageLogService {

    private static final String KEY = "toffco:whatsapp:message-log";
    private static final int MAX_MESSAGES = 200;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    private final StringRedisTemplate redisTemplate;

    public MessageLogService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Registra uma mensagem recebida do cliente.
     */
    public void logReceived(String number, String text) {
        String entry = formatEntry("⬅️ RECEBIDA", number, text);
        push(entry);
    }

    /**
     * Registra uma mensagem enviada pelo bot.
     */
    public void logSent(String number, String text) {
        String entry = formatEntry("➡️ ENVIADA", number, text);
        push(entry);
    }

    /**
     * Registra um evento do sistema (atendimento humano, bloqueio, etc.)
     */
    public void logEvent(String number, String event) {
        String entry = formatEntry("⚙️ EVENTO", number, event);
        push(entry);
    }

    /**
     * Retorna as últimas mensagens (mais recente primeiro).
     */
    public List<String> getRecentMessages() {
        List<String> messages = redisTemplate.opsForList().range(KEY, 0, MAX_MESSAGES - 1);
        return messages != null ? messages : List.of();
    }

    /**
     * Limpa o log.
     */
    public void clear() {
        redisTemplate.delete(KEY);
    }

    private void push(String entry) {
        redisTemplate.opsForList().leftPush(KEY, entry);
        redisTemplate.opsForList().trim(KEY, 0, MAX_MESSAGES - 1);
    }

    private String formatEntry(String direction, String number, String text) {
        String time = FMT.format(Instant.now());
        String shortText = text != null && text.length() > 150
                ? text.substring(0, 150) + "..."
                : (text != null ? text : "-");
        // Escapa HTML
        shortText = shortText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return time + " | " + direction + " | " + number + " | " + shortText;
    }
}
