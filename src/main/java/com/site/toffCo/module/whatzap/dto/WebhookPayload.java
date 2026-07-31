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
    public record WebhookKey(
            String remoteJid,
            String remoteJidAlt,
            String senderPn,
            Boolean fromMe,
            String id
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookMessage(
            // ─── TEXTO ────────────────────────────────────────────────
            String conversation,
            ExtendedTextMessage extendedTextMessage,

            // ─── MÍDIAS ───────────────────────────────────────────────
            // A Evolution manda um objeto para cada tipo.
            // Usamos Object porque só precisamos saber se o campo existe,
            // não do conteúdo em si.
            Object audioMessage,
            Object imageMessage,
            Object videoMessage,
            Object documentMessage,
            Object stickerMessage
    ) {
        /** Retorna o texto da mensagem, ou null se for mídia/vazio. */
        public String text() {
            if (conversation != null && !conversation.isBlank()) {
                return conversation;
            }
            return extendedTextMessage == null ? null : extendedTextMessage.text();
        }

        /**
         * Detecta o tipo de mídia recebida.
         * Retorna o enum correspondente, ou TEXT se for mensagem de texto normal.
         */
        public MediaType mediaType() {
            if (audioMessage    != null) return MediaType.AUDIO;
            if (imageMessage    != null) return MediaType.IMAGE;
            if (videoMessage    != null) return MediaType.VIDEO;
            if (documentMessage != null) return MediaType.DOCUMENT;
            if (stickerMessage  != null) return MediaType.STICKER;
            return MediaType.TEXT;
        }

        /** Retorna true se a mensagem NÃO é texto — o bot não consegue processar. */
        public boolean isMedia() {
            return mediaType() != MediaType.TEXT;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtendedTextMessage(String text) {}

    public enum MediaType {
        TEXT, AUDIO, IMAGE, VIDEO, DOCUMENT, STICKER;

        /** Mensagem amigável para o cliente explicando o que foi recebido. */
        public String friendlyName() {
            return switch (this) {
                case AUDIO    -> "áudio 🎵";
                case IMAGE    -> "imagem 📷";
                case VIDEO    -> "vídeo 🎬";
                case DOCUMENT -> "documento 📄";
                case STICKER  -> "sticker 😄";
                case TEXT     -> "texto";
            };
        }
    }
}
