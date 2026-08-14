package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.infra.config.MetaWhatsappProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook/meta/whatsapp")
@RequiredArgsConstructor
public class MetaWhatsappWebhookController {

    private final MetaWhatsappProperties properties;
    private final ObjectMapper objectMapper;
    private final WhatsAppController legacyController;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (!properties.enabled() || !properties.isWebhookConfigured()) {
            return ResponseEntity.notFound().build();
        }

        if ("subscribe".equals(mode)
                && constantTimeEquals(properties.verifyToken(), verifyToken)
                && challenge != null) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (!properties.enabled() || !properties.isWebhookConfigured()) {
            return ResponseEntity.notFound().build();
        }

        if (!validSignature(rawBody, signature)) {
            log.warn("Webhook Meta rejeitado: assinatura inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            int processed = forwardMessages(root);
            log.debug("Webhook Meta processado: {} mensagem(ns)", processed);
            return ResponseEntity.ok().build();
        } catch (Exception exception) {
            log.error("Falha ao processar webhook Meta", exception);
            return ResponseEntity.badRequest().build();
        }
    }

    private int forwardMessages(JsonNode root) {
        int processed = 0;
        JsonNode entries = root.path("entry");

        if (!entries.isArray()) {
            return 0;
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) {
                continue;
            }

            for (JsonNode change : changes) {
                if (!"messages".equals(change.path("field").asText())) {
                    continue;
                }

                JsonNode messages = change.path("value").path("messages");
                if (!messages.isArray()) {
                    continue;
                }

                for (JsonNode message : messages) {
                    if (forwardMessage(message)) {
                        processed++;
                    }
                }
            }
        }

        return processed;
    }

    private boolean forwardMessage(JsonNode message) {
        String from = message.path("from").asText("");
        String messageId = message.path("id").asText("");
        String type = message.path("type").asText("");

        if (from.isBlank() || messageId.isBlank() || type.isBlank()) {
            return false;
        }

        String text = extractText(message, type);
        String mediaField = mediaField(type);

        Map<String, Object> key = new LinkedHashMap<>();
        key.put("remoteJid", from + "@s.whatsapp.net");
        key.put("remoteJidAlt", null);
        key.put("senderPn", from);
        key.put("originalLid", null);
        key.put("fromMe", false);
        key.put("id", messageId);

        Map<String, Object> normalizedMessage = new LinkedHashMap<>();
        if (text != null && !text.isBlank()) {
            normalizedMessage.put("conversation", text);
        } else if (mediaField != null) {
            normalizedMessage.put(mediaField, Map.of());
        } else {
            return false;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("message", normalizedMessage);

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put("event", "messages.upsert");
        normalizedPayload.put("data", data);

        JsonNode legacyPayload = objectMapper.valueToTree(normalizedPayload);
        legacyController.receiveMessage(legacyPayload);
        return true;
    }

    private String extractText(JsonNode message, String type) {
        return switch (type) {
            case "text" -> message.path("text").path("body").asText(null);
            case "button" -> message.path("button").path("text").asText(null);
            case "interactive" -> {
                JsonNode interactive = message.path("interactive");
                String button = interactive.path("button_reply").path("title").asText(null);
                yield button != null
                        ? button
                        : interactive.path("list_reply").path("title").asText(null);
            }
            case "image", "video", "document" -> message.path(type).path("caption").asText(null);
            default -> null;
        };
    }

    private String mediaField(String type) {
        return switch (type) {
            case "audio" -> "audioMessage";
            case "image" -> "imageMessage";
            case "video" -> "videoMessage";
            case "document" -> "documentMessage";
            case "sticker" -> "stickerMessage";
            default -> null;
        };
    }

    private boolean validSignature(String rawBody, String signature) {
        if (signature == null || signature.isBlank()
                || properties.appSecret() == null
                || properties.appSecret().isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.appSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            String expected = "sha256=" + HexFormatHolder.hex(mac.doFinal(
                    rawBody.getBytes(StandardCharsets.UTF_8)
            ));
            return constantTimeEquals(expected, signature);
        } catch (Exception exception) {
            log.error("Não foi possível validar a assinatura do webhook Meta", exception);
            return false;
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static final class HexFormatHolder {
        private static String hex(byte[] bytes) {
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(value & 0x0f, 16));
            }
            return result.toString();
        }
    }
}
