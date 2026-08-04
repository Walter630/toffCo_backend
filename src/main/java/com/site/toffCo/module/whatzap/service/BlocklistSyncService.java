package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlocklistSyncService {

    private final WhatsappProperties whatsappProperties;
    private final WhatsappSessionStore sessionStore;

    @Value("${whatsapp.bridge.url:http://localhost:3100}")
    private String bridgeUrl;

    @Value("${whatsapp.bridge.secret:}")
    private String bridgeSecret;

    @EventListener(ApplicationReadyEvent.class)
    @Async("whatsappBotExecutor")
    public void syncBlocklistToRedis() {
        var blockedNumbers = whatsappProperties.blockedNumbers();

        if (blockedNumbers == null || blockedNumbers.isEmpty()) {
            log.info("Nenhum número bloqueado configurado no .env");
            return;
        }

        // 1. Bloqueia os números reais no Redis (fallback)
        int directCount = 0;
        for (String number : blockedNumbers) {
            if (number != null && !number.isBlank()) {
                sessionStore.blockNumber(number);
                directCount++;
            }
        }
        log.info("Blocklist: {} números reais adicionados ao Redis", directCount);

        // 2. Espera o bridge conectar no WhatsApp
        sleep(15_000);

        // 3. Resolve números → LIDs via bridge
        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            var requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(60));

            RestClient client = RestClient.builder()
                    .baseUrl(bridgeUrl)
                    .defaultHeader("X-Bridge-Secret", bridgeSecret)
                    .defaultHeader("Content-Type", "application/json")
                    .requestFactory(requestFactory)
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/resolve-numbers")
                    .body(Map.of("numbers", blockedNumbers))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("mappings")) {
                @SuppressWarnings("unchecked")
                Map<String, String> mappings = (Map<String, String>) response.get("mappings");

                int lidCount = 0;
                for (var entry : mappings.entrySet()) {
                    String lid = entry.getValue();
                    if (lid != null && !lid.isBlank()) {
                        sessionStore.blockNumber(lid);
                        lidCount++;
                    }
                }
                log.info("Blocklist: {} LIDs resolvidos e bloqueados no Redis", lidCount);
            }
        } catch (Exception exception) {
            log.warn("Falha ao resolver LIDs via bridge: {}", exception.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
