package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.config.EvolutionApiProperties;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class WhatzapService {

    private final RestClient restClient;
    private final RestClient presenceClient;
    private final String instanceName;
    private final WhatsappCircuitBreaker circuitBreaker;
    private final WhatsappMonitoringService monitoring;
    private final RestClient n8nClient;
    private final String n8nAlertWebhookUrl;
    private final boolean n8nReviewMode;

    /**
     * Injeta as configurações via @ConfigurationProperties (type-safe).
     *
     * JdkClientHttpRequestFactory usa o HttpClient nativo do Java (Java 11+),
     * sem nenhuma dependência extra no pom.xml. Com Virtual Threads habilitadas
     * (spring.threads.virtual.enabled=true), bloquear aqui é seguro e eficiente
     * — cada requisição roda numa virtual thread barata, sem ocupar plataforma thread.
     */
    public WhatzapService(EvolutionApiProperties props,
                          @Value("${evolution.api.read-timeout:PT8S}") Duration readTimeout,
                          @Value("${n8n.alert-webhook-url:}") String n8nAlertWebhookUrl,
                          @Value("${n8n.review-mode:false}") boolean n8nReviewMode,
                          WhatsappCircuitBreaker circuitBreaker,
                          WhatsappMonitoringService monitoring) {
        log.info("WhatzapService iniciando — url={}, instance={}", props.url(), props.instance());

        this.instanceName = props.instance();
        this.circuitBreaker = circuitBreaker;
        this.monitoring = monitoring;
        this.n8nAlertWebhookUrl = n8nAlertWebhookUrl;
        this.n8nReviewMode = n8nReviewMode;

        // HttpClient nativo do Java — connection pooling gerenciado pela JVM
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        var presenceRequestFactory = new JdkClientHttpRequestFactory(httpClient);
        presenceRequestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .baseUrl(props.url())
                .defaultHeader("apikey", props.key())
                .requestFactory(requestFactory)
                .build();
        this.presenceClient = RestClient.builder()
                .baseUrl(props.url())
                .defaultHeader("apikey", props.key())
                .requestFactory(presenceRequestFactory)
                .build();
        this.n8nClient = RestClient.builder().build();
    }

    /**
     * Envia uma única mensagem via Evolution API.
     *
     * Erros de rede ou resposta HTTP 4xx/5xx são logados mas não relançados,
     * porque o webhook da Evolution não deve receber 500 — ele retentaria
     * indefinidamente caso o nosso servidor retornasse erro.
     */
    public boolean sendMessage(SendMessageRequest request) {
        if (!circuitBreaker.allowRequest()) {
            monitoring.recordCircuitBlocked();
            log.warn("Envio WhatsApp bloqueado pelo circuit breaker para {}", request.number());
            notifyN8n("WHATSAPP_CIRCUIT_OPEN", request.number(), "Circuit breaker bloqueou o envio");
            return false;
        }
        long startedAt = monitoring.startTimer();
        String url = "/message/sendText/" + this.instanceName;
        log.info("Enviando mensagem → number={}, url={}", request.number(), url);

        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            log.info("Evolution API response para {}: {}", request.number(), responseBody);
            circuitBreaker.recordSuccess();
            monitoring.recordSuccess(startedAt);
            return true;

        } catch (RestClientResponseException e) {
            log.error("Erro HTTP da Evolution API: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            circuitBreaker.recordFailure();
            monitoring.recordFailure(startedAt);
            notifyN8n("WHATSAPP_SEND_FAILURE", request.number(), "Evolution retornou HTTP " + e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Falha de rede ao contactar Evolution API para number={}",
                    request.number(), e);
            circuitBreaker.recordFailure();
            monitoring.recordFailure(startedAt);
            notifyN8n("WHATSAPP_SEND_FAILURE", request.number(), "Falha de rede: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /** Mostra "digitando..." sem deixar uma falha de presença impedir a resposta. */
    private void notifyN8n(String type, String number, String message) {
        if (n8nAlertWebhookUrl == null || n8nAlertWebhookUrl.isBlank()) return;
        try {
            n8nClient.post().uri(n8nAlertWebhookUrl).body(Map.of(
                    "type", type,
                    "number", number == null ? "" : number,
                    "message", message,
                    "timestamp", java.time.OffsetDateTime.now().toString()
            )).retrieve().toBodilessEntity();
        } catch (Exception alertException) {
            log.warn("Não foi possível enviar alerta ao n8n: {}", alertException.getMessage());
        }
    }

    /** Envia uma cópia das respostas para revisão manual durante a fase de testes. */
    public void notifyBotResponseReview(String number, String incomingMessage,
                                        String botResponse, String state) {
        if (!n8nReviewMode || n8nAlertWebhookUrl == null || n8nAlertWebhookUrl.isBlank()) {
            return;
        }
        try {
            n8nClient.post().uri(n8nAlertWebhookUrl).body(Map.of(
                    "type", "BOT_RESPONSE_REVIEW",
                    "number", number == null ? "" : number,
                    "incomingMessage", incomingMessage == null ? "" : incomingMessage,
                    "botResponse", botResponse == null ? "" : botResponse,
                    "state", state == null ? "" : state,
                    "timestamp", java.time.OffsetDateTime.now().toString()
            )).retrieve().toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Não foi possível enviar revisão do bot ao n8n: {}", exception.getMessage());
        }
    }

    /** Mostra "digitando..." sem deixar uma falha de presença impedir a resposta. */
    public void sendTyping(String number) {
        if (number == null || number.isBlank()) {
            return;
        }
        try {
            presenceClient.post()
                    .uri("/chat/sendPresence/" + this.instanceName)
                    .body(Map.of(
                            "number", number,
                            "presence", "composing",
                            "delay", 5000
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.debug("Não foi possível mostrar status digitando para {}: {}", number,
                    exception.getMessage());
        }
    }

    /**
     * Envia múltiplas mensagens em paralelo usando StructuredTaskScope (Java 25 preview).
     *
     * API do Java 25 (JEP 505): usa StructuredTaskScope.open() com Joiner.
     *  - ShutdownOnFailure não existe mais — substituído por Joiner.awaitAllSuccessfulOrThrow()
     *  - Se qualquer subtask falhar, o scope cancela as demais e join() lança FailedException
     *  - Cada fork() cria uma virtual thread — sem custo de thread pool
     *
     * Por que não CompletableFuture?
     *  - Structured concurrency garante que o bloco só termina quando TODAS as tasks terminam
     *  - Sem vazamento: se uma falha, o scope cancela as demais automaticamente
     *  - Leitura linear — sem callbacks encadeados
     */
    @SuppressWarnings("preview")
    public void sendMessages(List<SendMessageRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        if (requests.size() == 1) {
            sendMessage(requests.getFirst());
            return;
        }

        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            requests.forEach(req -> scope.fork(() -> {
                sendMessage(req);
                return null;
            }));

            scope.join();

        } catch (StructuredTaskScope.FailedException e) {
            log.error("Falha em uma das mensagens paralelas", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Envio paralelo interrompido", e);
        } catch (Exception e) {
            log.error("Erro inesperado no envio paralelo de mensagens", e);
        }
    }
}
