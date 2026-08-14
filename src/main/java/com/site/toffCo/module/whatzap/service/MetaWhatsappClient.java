package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.config.MetaWhatsappProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class MetaWhatsappClient {

    private final MetaWhatsappProperties properties;
    private final RestClient restClient;

    public MetaWhatsappClient(
            MetaWhatsappProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;

        Duration timeout = properties.readTimeout() == null
                ? Duration.ofSeconds(8)
                : properties.readTimeout();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        this.restClient = restClientBuilder
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public String sendText(String number, String text) {
        if (!properties.isSendConfigured()) {
            throw new IllegalStateException(
                    "WhatsApp Cloud API habilitada sem META_PHONE_NUMBER_ID "
                            + "ou META_ACCESS_TOKEN"
            );
        }

        String recipient = number == null ? "" : number.replaceAll("\\D", "");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("Número de destino WhatsApp inválido");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Texto da mensagem WhatsApp vazio");
        }

        return restClient.post()
                .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
                .header("Authorization", "Bearer " + properties.accessToken())
                .body(Map.of(
                        "messaging_product", "whatsapp",
                        "recipient_type", "individual",
                        "to", recipient,
                        "type", "text",
                        "text", Map.of(
                                "preview_url", false,
                                "body", text
                        )
                ))
                .retrieve()
                .body(String.class);
    }
}
