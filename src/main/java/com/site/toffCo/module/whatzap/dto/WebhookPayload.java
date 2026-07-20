package com.site.toffCo.module.whatzap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Anotações (@JsonIgnoreProperties, @JsonProperty, etc.) continuam em
 * com.fasterxml.jackson.annotation mesmo no Jackson 3.
 * Apenas o core da API (JsonNode, ObjectMapper, databind) migrou para tools.jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(WebhookData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookData(WebhookKey key, WebhookMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookKey(String remoteJid, String remoteJidAlt, Boolean fromMe, String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookMessage(
            String conversation,
            ExtendedTextMessage extendedTextMessage
    ) {
        public String text() {
            if (conversation != null && !conversation.isBlank()) {
                return conversation;
            }
            return extendedTextMessage == null ? null : extendedTextMessage.text();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtendedTextMessage(String text) {}
}
