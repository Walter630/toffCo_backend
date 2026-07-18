package com.site.toffCo.module.whatzap.session;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.ChatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

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

    // ─── BUSCAR UMA SESSÃO ─────────────────────────────────────

    public Optional<WhatsappSession> findByWhatsappId(String whatsappId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(whatsappId));

        if (values.isEmpty()) {
            return Optional.empty();
        }

        try {
            String lastMessageId = optional(values, "lastMessageId");
            String lastBotReplyAtRaw = optional(values, "lastBotReplyAt");
            Instant lastBotReplyAt = parseInstant(lastBotReplyAtRaw);

            String humanAssignedAtRaw = optional(values, "humanAssignedAt");
            Instant humanAssignedAt = parseInstant(humanAssignedAtRaw);
            String resolvedBy =  optional(values, "resolvedBy");

            String statusRaw = optional(values, "status");
            ChatStatus status = statusRaw != null ? ChatStatus.valueOf(statusRaw) : null;
            return Optional.of(new WhatsappSession(
                    whatsappId,
                    ChatState.valueOf(required(values, "currentState")),
                    Integer.parseInt(required(values, "currentPage")),
                    Boolean.parseBoolean(required(values, "humanAssigned")),
                    lastMessageId,
                    lastBotReplyAt,
                    optional(values, "attendanceSubject"),
                    // ─── CAMPOS NOVOS ─────────────────────────
                    humanAssignedAt,
                    optional(values, "lastMessage"),
                    optional(values, "assignedTo"),
                    status,
                    resolvedBy
            ));
        } catch (IllegalArgumentException exception) {
            delete(whatsappId);
            return Optional.empty();
        }
    }

    // ─── LISTAR TODAS AS SESSÕES (pro dashboard e /pendentes) ──

    public List<WhatsappSession> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<WhatsappSession> sessions = new ArrayList<>();
        for (String redisKey : keys) {
            String whatsappId = redisKey.replace(KEY_PREFIX, "");
            findByWhatsappId(whatsappId).ifPresent(sessions::add);
        }
        return sessions;
    }

    // ─── FILTRAR POR ATENDENTE (pra quando tiver multi-atendente)

    public List<WhatsappSession> findByAssignedTo(String attendantWhatsappId) {
        return findAll().stream()
                .filter(s -> attendantWhatsappId.equals(s.getAssignedTo()))
                .toList();
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

        // ─── CAMPOS NOVOS ───────────────────────────────────────
        if (session.getHumanAssingnedAt() != null) {
            fields.put("humanAssignedAt", session.getHumanAssingnedAt().toString());
        }
        if (session.getLastMessage() != null) {
            fields.put("lastMessage", session.getLastMessage());
        }
        if (session.getAssignedTo() != null) {
            fields.put("assignedTo", session.getAssignedTo());
        }
        if (session.getStatus() != null) {
            fields.put("status", session.getStatus().name());
        }
        if (session.getResolvedBy() != null) {
            fields.put("resolvedBy", session.getResolvedBy());
        }
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, sessionTtl);
    }

    // ─── DELETAR ───────────────────────────────────────────────

    public void delete(String whatsappId) {
        redisTemplate.delete(key(whatsappId));
    }

    // ─── HELPERS ───────────────────────────────────────────────

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

    private Instant parseInstant(String raw) {
        return raw == null ? null : Instant.parse(raw);
    }
}
