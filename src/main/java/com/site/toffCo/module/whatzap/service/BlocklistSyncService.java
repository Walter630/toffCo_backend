package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Sincroniza os números bloqueados do .env (WHATSAPP_BLOCKED_NUMBERS)
 * para o Redis na inicialização da aplicação.
 *
 * Isso garante que a blocklist funciona independentemente de como
 * o número chega (LID ou número real) — basta estar no Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlocklistSyncService {

    private final WhatsappProperties whatsappProperties;
    private final WhatsappSessionStore sessionStore;

    @EventListener(ApplicationReadyEvent.class)
    public void syncBlocklistToRedis() {
        var blockedNumbers = whatsappProperties.blockedNumbers();

        if (blockedNumbers == null || blockedNumbers.isEmpty()) {
            log.info("Nenhum número bloqueado configurado no .env");
            return;
        }

        int count = 0;
        for (String number : blockedNumbers) {
            if (number != null && !number.isBlank()) {
                sessionStore.blockNumber(number);
                count++;
            }
        }

        log.info("Blocklist sincronizada: {} números do .env adicionados ao Redis", count);
    }
}
