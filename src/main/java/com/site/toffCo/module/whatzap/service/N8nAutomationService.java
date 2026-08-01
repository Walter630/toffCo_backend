package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Publica eventos de automação sem bloquear o fluxo do WhatsApp. */
@Slf4j
@Service
public class N8nAutomationService {

    private final RestClient client;
    private final String webhookUrl;
    private final String webhookSecret;
    private final String transport;
    private final AmqpTemplate amqpTemplate;

    public N8nAutomationService(
            @Value("${n8n.alert-webhook-url:}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret,
            @Value("${n8n.connect-timeout:PT1S}") Duration connectTimeout,
            @Value("${n8n.read-timeout:PT2S}") Duration readTimeout,
            @Value("${n8n.transport:webhook}") String transport,
            AmqpTemplate amqpTemplate
    ) {
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
        this.transport = transport == null ? "webhook" : transport;
        this.amqpTemplate = amqpTemplate;

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Async("n8nEventExecutor")
    public void publish(String type, String eventId, Map<String, Object> data) {
        N8nAutomationEvent event = new N8nAutomationEvent(
                eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId,
                type,
                "toffco-backend",
                OffsetDateTime.now(),
                data == null ? Map.of() : Map.copyOf(data)
        );

        if ("rabbitmq".equalsIgnoreCase(transport)) {
            publishToRabbit(event, type);
            return;
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        publishToWebhook(event, type);
    }

    private void publishToRabbit(N8nAutomationEvent event, String type) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                amqpTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.N8N_AUTOMATION_ROUTING_KEY,
                        event
                );
                return;
            } catch (Exception exception) {
                if (attempt == 2) {
                    log.warn("Evento {} não chegou ao RabbitMQ após {} tentativas: {}",
                            type, attempt, exception.getMessage());
                }
            }
        }
    }

    private void publishToWebhook(N8nAutomationEvent event, String type) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                client.post()
                        .uri(webhookUrl)
                        .headers(headers -> {
                            if (!webhookSecret.isBlank()) {
                                headers.set("X-N8N-Webhook-Secret", webhookSecret);
                            }
                        })
                        .body(event)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (Exception exception) {
                if (attempt == 2) {
                    log.warn("Evento {} não chegou ao n8n após {} tentativas: {}",
                            type, attempt, exception.getMessage());
                }
            }
        }
    }
}
