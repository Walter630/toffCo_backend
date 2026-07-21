package com.site.toffCo.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.util.List;

/**
 * Configurações do módulo WhatsApp/bot.
 *
 * blocked-numbers: números fixos que o bot NUNCA responde.
 *   Configurar no .env: WHATSAPP_BLOCKED_NUMBERS=553488560330,551199999999
 *   Separe por vírgula. Para bloqueios em runtime use /bloquear no WhatsApp.
 */
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsappProperties(
        Session session,

        @NotBlank(message = "WHATSAPP_ATTENDANT_NUMBER não configurado no .env")
        String attendantNumber,

        List<String> blockedNumbers
) {
    public record Session(Duration ttl) {}

    /** Retorna true se o número está na lista estática do .env */
    public boolean isStaticallyBlocked(String number) {
        if (blockedNumbers == null || number == null) return false;
        String clean = number.replaceAll("\\D", "");
        return blockedNumbers.stream()
                .map(n -> n.replaceAll("\\D", ""))
                .anyMatch(clean::equals);
    }
}
