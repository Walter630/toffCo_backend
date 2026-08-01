package com.site.toffCo.module.whatzap.service;

import java.time.OffsetDateTime;
import java.util.Map;

/** Contrato único de eventos entre o backend e o n8n. */
public record N8nAutomationEvent(
        String eventId,
        String type,
        String source,
        OffsetDateTime occurredAt,
        Map<String, Object> data
) {}
