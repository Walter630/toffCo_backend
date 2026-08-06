package com.site.toffCo.module.whatzap.monitoring;

import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import com.site.toffCo.module.whatzap.service.N8nAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watchdog de blocklist: monitora continuamente se todos os números
 * bloqueados estão de fato ativos no Redis. Se algum sumir (por flush,
 * restart parcial, expiração acidental), re-bloqueia automaticamente
 * e envia alerta pro seu WhatsApp via N8N.
 *
 * Também rastreia violações do GUARD FINAL — se o guard for acionado,
 * significa que algo quase passou pelas camadas anteriores.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlocklistWatchdog {

    private final WhatsappProperties whatsappProperties;
    private final WhatsappSessionStore sessionStore;
    private final N8nAutomationService n8nService;

    @Value("${whatsapp.attendant-number:}")
    private String attendantNumber;

    // Contadores de saúde
    private final AtomicLong guardFinalViolations = new AtomicLong(0);
    private final AtomicLong autoRepairCount = new AtomicLong(0);
    private Instant lastCheckAt;
    private boolean lastCheckHealthy = true;

    /**
     * Registra uma violação do GUARD FINAL.
     * Chamado pelo ChatBotService quando o guard impede um envio.
     * Envia alerta imediato via N8N.
     */
    public void recordGuardFinalViolation(String number) {
        long count = guardFinalViolations.incrementAndGet();
        log.error(
                "ALERTA WATCHDOG: GUARD FINAL acionado! Número bloqueado {} quase recebeu mensagem. "
                        + "Total de violações desde o boot: {}",
                number,
                count
        );

        // Alerta imediato via N8N → chega no seu WhatsApp
        n8nService.publish(
                "blocklist.guard_final_violation",
                null,
                Map.of(
                        "number", number,
                        "totalViolations", count,
                        "message", "ALERTA: O GUARD FINAL impediu envio para número bloqueado " + number
                                + ". Isso significa que as camadas anteriores falharam. Verifique os logs.",
                        "severity", "critical"
                )
        );
    }

    /**
     * Verificação periódica a cada 2 minutos.
     * Garante que TODOS os números da lista estática estão no Redis.
     * Se algum sumiu, re-adiciona automaticamente e te avisa.
     */
    @Scheduled(fixedDelay = 120_000, initialDelay = 30_000)
    public void verifyBlocklistIntegrity() {
        List<String> blockedNumbers = whatsappProperties.blockedNumbers();

        if (blockedNumbers == null || blockedNumbers.isEmpty()) {
            return;
        }

        int total = blockedNumbers.size();
        int presentInRedis = 0;
        List<String> missing = new ArrayList<>();

        for (String number : blockedNumbers) {
            if (sessionStore.isBlocked(number)) {
                presentInRedis++;
            } else {
                missing.add(number);
            }
        }

        lastCheckAt = Instant.now();

        if (missing.isEmpty()) {
            lastCheckHealthy = true;
            log.info(
                    "WATCHDOG OK: Blocklist íntegra — {}/{} números ativos no Redis. "
                            + "Guard violations desde boot: {}",
                    presentInRedis,
                    total,
                    guardFinalViolations.get()
            );
        } else {
            lastCheckHealthy = false;
            log.error(
                    "WATCHDOG ALERTA: {}/{} números AUSENTES do Redis! Re-bloqueando automaticamente: {}",
                    missing.size(),
                    total,
                    missing
            );

            // Auto-repair: re-adiciona os números que sumiram
            for (String number : missing) {
                sessionStore.blockNumber(number);
                autoRepairCount.incrementAndGet();
            }

            log.info(
                    "WATCHDOG: Auto-repair concluído. {} números re-adicionados ao Redis.",
                    missing.size()
            );

            // Alerta via N8N
            n8nService.publish(
                    "blocklist.auto_repair",
                    null,
                    Map.of(
                            "missingCount", missing.size(),
                            "totalConfigured", total,
                            "repairedNumbers", missing.toString(),
                            "message", "WATCHDOG: " + missing.size() + " números sumiram do Redis e foram "
                                    + "re-adicionados automaticamente. Verifique se o Redis está estável.",
                            "severity", "warning"
                    )
            );
        }
    }

    /**
     * Snapshot para exibição no dashboard.
     */
    public WatchdogSnapshot snapshot() {
        List<String> blockedNumbers = whatsappProperties.blockedNumbers();
        int total = blockedNumbers != null ? blockedNumbers.size() : 0;

        int inRedis = 0;
        if (blockedNumbers != null) {
            for (String number : blockedNumbers) {
                if (sessionStore.isBlocked(number)) {
                    inRedis++;
                }
            }
        }

        return new WatchdogSnapshot(
                total,
                inRedis,
                lastCheckHealthy,
                lastCheckAt,
                guardFinalViolations.get(),
                autoRepairCount.get()
        );
    }

    public record WatchdogSnapshot(
            int totalConfigured,
            int activeInRedis,
            boolean healthy,
            Instant lastCheckAt,
            long guardFinalViolations,
            long autoRepairs
    ) {}
}
