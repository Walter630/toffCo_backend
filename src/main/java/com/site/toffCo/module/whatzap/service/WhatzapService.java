package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

/**
 * Serviço de envio de mensagens WhatsApp.
 *
 * ANTES: chamava a Evolution API (intermediário HTTP entre backend e WhatsApp).
 * AGORA: chama o whatsapp-bridge (Node.js com Baileys direto no WhatsApp).
 *
 * A interface pública (sendMessage, sendTyping, etc.) não mudou —
 * o ChatBotService continua chamando exatamente os mesmos métodos.
 */
@Slf4j
@Service
public class WhatzapService {

    private final RestClient restClient;
    private final RestClient presenceClient;
    private final WhatsappCircuitBreaker circuitBreaker;
    private final WhatsappMonitoringService monitoring;
    private final N8nAutomationService n8nAutomationService;
    private final boolean n8nReviewMode;

    public WhatzapService(
            @Value("${whatsapp.bridge.url:http://localhost:3100}") String bridgeUrl,
            @Value("${whatsapp.bridge.secret:}") String bridgeSecret,
            @Value("${whatsapp.bridge.read-timeout:PT8S}") Duration readTimeout,
            @Value("${n8n.review-mode:false}") boolean n8nReviewMode,
            WhatsappCircuitBreaker circuitBreaker,
            WhatsappMonitoringService monitoring,
            N8nAutomationService n8nAutomationService
    ) {
        log.info("WhatzapService iniciando - bridge={}", bridgeUrl);

        this.circuitBreaker = circuitBreaker;
        this.monitoring = monitoring;
        this.n8nAutomationService = n8nAutomationService;
        this.n8nReviewMode = n8nReviewMode;

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        var presenceRequestFactory = new JdkClientHttpRequestFactory(httpClient);
        presenceRequestFactory.setReadTimeout(Duration.ofSeconds(3));

        // Client principal — manda mensagens pro bridge
        this.restClient = RestClient.builder()
                .baseUrl(bridgeUrl)
                .defaultHeader("X-Bridge-Secret", bridgeSecret)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();

        // Client de presença — timeout mais curto (não é crítico)
        this.presenceClient = RestClient.builder()
                .baseUrl(bridgeUrl)
                .defaultHeader("X-Bridge-Secret", bridgeSecret)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(presenceRequestFactory)
                .build();
    }

    /**
     * Envia uma mensagem de texto via WhatsApp Bridge.
     *
     * O bridge recebe: { "number": "...", "text": "...", "delay": 2200 }
     * E manda direto pro WhatsApp via Baileys (sem intermediário).
     */
    public boolean sendMessage(SendMessageRequest request) {
        if (!circuitBreaker.allowRequest()) {
            monitoring.recordCircuitBlocked();
            log.warn("Envio WhatsApp bloqueado pelo circuit breaker para {}", request.number());
            publishAutomationEvent("WHATSAPP_CIRCUIT_OPEN", eventWindowId("circuit-open"),
                    Map.of("message", "Circuit breaker bloqueou o envio"));
            return false;
        }

        long startedAt = monitoring.startTimer();

        try {
            // O bridge espera: { number, text, delay }
            // SendMessageRequest já tem esses campos — manda direto
            String responseBody = restClient.post()
                    .uri("/send-message")
                    .body(Map.of(
                            "number", request.number(),
                            "text", request.text(),
                            "delay", request.delay()
                    ))
                    .retrieve()
                    .body(String.class);

            log.debug("Bridge respondeu para {}: {}", request.number(), responseBody);
            circuitBreaker.recordSuccess();
            monitoring.recordSuccess(startedAt);
            return true;
        } catch (RestClientResponseException exception) {
            circuitBreaker.recordFailure();
            monitoring.recordFailure(startedAt);

            int status = exception.getStatusCode().value();

            // 503 = bridge desconectado do WhatsApp (não é falha permanente)
            if (status == 503) {
                log.warn("Bridge desconectado do WhatsApp ao enviar para {}", request.number());
            } else {
                log.error("Bridge recusou mensagem para {}: status={}, body={}",
                        request.number(), status, exception.getResponseBodyAsString());
            }

            publishAutomationEvent("WHATSAPP_SEND_FAILURE", eventWindowId("send-failure:" + request.number()),
                    Map.of("message", "Bridge retornou HTTP " + status));
            return false;
        } catch (Exception exception) {
            circuitBreaker.recordFailure();
            monitoring.recordFailure(startedAt);
            log.error("Falha de rede ao contactar WhatsApp Bridge para {}", request.number(), exception);
            publishAutomationEvent("WHATSAPP_SEND_FAILURE", eventWindowId("send-failure:" + request.number()),
                    Map.of("message", "Falha de rede: " + exception.getClass().getSimpleName()));
            return false;
        }
    }

    public void publishAutomationEvent(String type, String eventId, Map<String, Object> data) {
        n8nAutomationService.publish(type, eventId, data);
    }

    private String eventWindowId(String prefix) {
        long fiveMinuteWindow = System.currentTimeMillis() / 300_000;
        return prefix + ":" + fiveMinuteWindow;
    }

    public void notifyBotResponseReview(
            String messageId,
            String number,
            String incomingMessage,
            String botResponse,
            String state
    ) {
        if (!n8nReviewMode) {
            return;
        }

        String eventId = messageId == null || messageId.isBlank()
                ? "bot-response-review:" + number + ":" +
                Integer.toHexString((incomingMessage + "|" + botResponse).hashCode())
                : "bot-response-review:" + messageId;

        publishAutomationEvent("BOT_RESPONSE_REVIEW", eventId, Map.of(
                "number", number == null ? "" : number,
                "incomingMessage", incomingMessage == null ? "" : incomingMessage,
                "botResponse", botResponse == null ? "" : botResponse,
                "state", state == null ? "" : state
        ));
    }

    /**
     * Mostra "digitando..." no chat do cliente.
     * Agora chama POST /send-presence no bridge.
     */
    public void sendTyping(String number) {
        if (number == null || number.isBlank()) {
            return;
        }

        try {
            presenceClient.post()
                    .uri("/send-presence")
                    .body(Map.of("number", number, "presence", "composing"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            // Presença não é crítico — se falhar, a mensagem vai igual
            log.debug("Falha ao mostrar digitando para {}: {}", number, exception.getMessage());
        }
    }

    @SuppressWarnings("preview")
    public void sendMessages(List<SendMessageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        if (requests.size() == 1) {
            sendMessage(requests.getFirst());
            return;
        }

        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            requests.forEach(request -> scope.fork(() -> {
                sendMessage(request);
                return null;
            }));
            scope.join();
        } catch (StructuredTaskScope.FailedException exception) {
            log.error("Falha em uma das mensagens paralelas", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Envio paralelo interrompido", exception);
        } catch (Exception exception) {
            log.error("Erro inesperado no envio paralelo de mensagens", exception);
        }
    }
}
