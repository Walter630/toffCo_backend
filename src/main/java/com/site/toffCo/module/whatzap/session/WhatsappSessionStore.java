package com.site.toffCo.module.whatzap.session;

import com.site.toffCo.module.whatzap.dto.ChatState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class WhatsappSessionStore {

    private static final String KEY_PREFIX = "toffco:whatsapp:session:";

    private final StringRedisTemplate redisTemplate;
    private final Duration sessionTtl;

    public WhatsappSessionStore(
            StringRedisTemplate redisTemplate,
            @Value("${whatsapp.session.ttl:PT24H}") Duration sessionTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.sessionTtl = sessionTtl;
    }

    public Optional<WhatsappSession> findByWhatsappId(String whatsappId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(whatsappId));

        if (values.isEmpty()) {
            return Optional.empty();
        }

        try {
            String lastMessageId = optional(values, "lastMessageId");
            String lastBotReplyAtRaw = optional(values, "lastBotReplyAt");
            Instant lastBotReplyAt = lastBotReplyAtRaw == null ? null : Instant.parse(lastBotReplyAtRaw);
            return Optional.of(new WhatsappSession(
                    whatsappId,
                    ChatState.valueOf(required(values, "currentState")),
                    Integer.parseInt(required(values, "currentPage")),
                    Boolean.parseBoolean(required(values, "humanAssigned")),
                    lastMessageId,
                    lastBotReplyAt,
                    optional(values, "attendanceSubject")
            ));
        } catch (IllegalArgumentException exception) {
            delete(whatsappId);
            return Optional.empty();
        }
    }

    public void save(WhatsappSession session) {
        String key = key(session.getWhatsappId());

        Map<String, String> fields = new java.util.HashMap<>(Map.of(
                "currentState", session.getCurrentState().name(),
                "currentPage", Integer.toString(session.getCurrentPage()),
                "humanAssigned", Boolean.toString(session.isHumanAssigned()),
                "attendanceSubject", session.getAttendanceSubject() == null ? "" : session.getAttendanceSubject()
        ));

        if (session.getLastMessageId() != null) {
            fields.put("lastMessageId", session.getLastMessageId());
        }
        if (session.getLastBotReplyAt() != null) {
            fields.put("lastBotReplyAt", session.getLastBotReplyAt().toString());
        }
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, sessionTtl);
    }
    public void delete(String whatsappId) {
        redisTemplate.delete(key(whatsappId));
    }

    private String key(String whatsappId) {
        return KEY_PREFIX + whatsappId;
    }

    private String required(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Campo ausente na sessão Redis: " + field);
        }
        return value.toString();
    }

    private String optional(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
