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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // Guarda os LIDs que já foram bloqueados para evitar logs repetitivos
    private final Set<String> knownBlockedLids = new HashSet<>();

    // ─── SYNC INICIAL (STARTUP) ───────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    @Async("whatsappBotExecutor")
    public void syncBlocklistToRedis() {
        List<String> blockedNumbers = whatsappProperties.blockedNumbers();

        if (blockedNumbers == null || blockedNumbers.isEmpty()) {
            log.info("Nenhum número bloqueado configurado no .env");
            return;
        }

        // 1. Bloqueia os números reais no Redis
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

        // 3. Tenta resolver números → LIDs via bridge (duas estratégias)
        syncLidsFromBridge(blockedNumbers);
    }

    // ─── SYNC PERIÓDICO (A CADA 5 MINUTOS) ───────────────────────

    /**
     * Re-sincroniza LIDs periodicamente.
     * O bridge pode descobrir novos mapeamentos conforme contatos são
     * carregados ou mensagens são trocadas. Este job garante que a
     * blocklist no Redis sempre tenha os LIDs mais recentes.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000) // 5 min, começa após 2 min
    public void periodicLidSync() {
        List<String> blockedNumbers = whatsappProperties.blockedNumbers();
        if (blockedNumbers == null || blockedNumbers.isEmpty()) {
            return;
        }

        log.debug("Blocklist sync periódico: verificando novos LIDs no bridge...");
        syncLidsFromBridge(blockedNumbers);
    }

    // ─── LÓGICA DE SYNC ───────────────────────────────────────────

    private void syncLidsFromBridge(List<String> blockedNumbers) {
        RestClient client = buildBridgeClient();
        if (client == null) return;

        int totalNewLids = 0;

        // Estratégia 1: Consulta /lid-mappings (mapeamentos já conhecidos pelo bridge)
        totalNewLids += syncFromLidMappings(client, blockedNumbers);

        // Estratégia 2: Pede pro bridge resolver via Baileys (/resolve-numbers)
        totalNewLids += syncFromResolveNumbers(client, blockedNumbers);

        if (totalNewLids > 0) {
            log.info("Blocklist: {} novos LIDs bloqueados no Redis neste ciclo", totalNewLids);
        } else {
            log.debug("Blocklist: nenhum LID novo encontrado neste ciclo");
        }
    }

    /**
     * Consulta GET /lid-mappings no bridge.
     * Retorna todos os mapeamentos LID↔número que o bridge já descobriu
     * via eventos de contatos do Baileys.
     */
    private int syncFromLidMappings(RestClient client, List<String> blockedNumbers) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.get()
                    .uri("/lid-mappings")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("mappings")) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> mappings = (Map<String, String>) response.get("mappings");

            // mappings é { lid: número }
            // Precisamos: para cada número bloqueado, ver se existe um LID correspondente
            int count = 0;
            Set<String> blockedSet = new HashSet<>(blockedNumbers);

            for (var entry : mappings.entrySet()) {
                String lid = entry.getKey();
                String number = entry.getValue();

                if (lid == null || number == null) continue;

                // Verifica se esse número está na blocklist (com normalização de 9o dígito)
                boolean shouldBlock = blockedSet.contains(number)
                        || whatsappProperties.isStaticallyBlocked(number);

                if (shouldBlock && !knownBlockedLids.contains(lid)) {
                    sessionStore.blockNumber(lid);
                    knownBlockedLids.add(lid);
                    count++;
                    log.info("Blocklist: LID {} (número {}) bloqueado via lid-mappings", lid, number);
                }
            }

            return count;
        } catch (Exception exception) {
            log.debug("Falha ao consultar /lid-mappings no bridge: {}", exception.getMessage());
            return 0;
        }
    }

    /**
     * Chama POST /resolve-numbers no bridge.
     * Pede pro Baileys verificar via onWhatsApp() quais números têm LID.
     */
    private int syncFromResolveNumbers(RestClient client, List<String> blockedNumbers) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/resolve-numbers")
                    .body(Map.of("numbers", blockedNumbers))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("mappings")) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> mappings = (Map<String, String>) response.get("mappings");

            int count = 0;
            for (var entry : mappings.entrySet()) {
                String lid = entry.getValue();
                if (lid != null && !lid.isBlank() && !knownBlockedLids.contains(lid)) {
                    sessionStore.blockNumber(lid);
                    knownBlockedLids.add(lid);
                    count++;
                    log.info("Blocklist: LID {} bloqueado via resolve-numbers", lid);
                }
            }

            return count;
        } catch (Exception exception) {
            log.debug("Falha ao resolver LIDs via /resolve-numbers: {}", exception.getMessage());
            return 0;
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private RestClient buildBridgeClient() {
        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            var requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(60));

            return RestClient.builder()
                    .baseUrl(bridgeUrl)
                    .defaultHeader("X-Bridge-Secret", bridgeSecret)
                    .defaultHeader("Content-Type", "application/json")
                    .requestFactory(requestFactory)
                    .build();
        } catch (Exception exception) {
            log.warn("Falha ao criar cliente para o bridge: {}", exception.getMessage());
            return null;
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
