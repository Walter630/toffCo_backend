package com.site.toffCo.infra.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsappProperties(
        Session session,

        @NotBlank(
                message = "WHATSAPP_ATTENDANT_NUMBER não configurado no .env"
        )
        String attendantNumber,

        List<String> blockedNumbers
) {

    public record Session(Duration ttl) {
    }

    /*
     * Normaliza a lista uma única vez quando a aplicação inicia.
     */
    public WhatsappProperties {
        blockedNumbers = blockedNumbers == null
                ? List.of()
                : blockedNumbers.stream()
                .filter(number -> number != null && !number.isBlank())
                .map(WhatsappProperties::normalize)
                .filter(number -> !number.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Verifica a lista estática configurada pelo .env.
     *
     * Também considera equivalentes números brasileiros
     * recebidos com ou sem o nono dígito.
     */
    public boolean isStaticallyBlocked(String number) {
        String receivedNumber = normalize(number);

        if (receivedNumber.isBlank()) {
            return false;
        }

        return blockedNumbers.stream()
                .anyMatch(blockedNumber ->
                        sameBrazilianWhatsappNumber(
                                blockedNumber,
                                receivedNumber
                        )
                );
    }

    private static String normalize(String number) {
        if (number == null) {
            return "";
        }

        return number.replaceAll("\\D", "");
    }

    private static boolean sameBrazilianWhatsappNumber(
            String first,
            String second
    ) {
        if (first.equals(second)) {
            return true;
        }

        /*
         * Compara também removendo o nono dígito.
         *
         * 5534984114981
         * 553484114981
         */
        return withoutBrazilianNinthDigit(first)
                .equals(withoutBrazilianNinthDigit(second));
    }

    private static String withoutBrazilianNinthDigit(String number) {
        /*
         * Formato:
         *
         * 55 + DDD + 9 + número
         *
         * Índices:
         * 0 1 = país
         * 2 3 = DDD
         * 4   = nono dígito
         */
        if (number.startsWith("55")
                && number.length() == 13
                && number.charAt(4) == '9') {

            return number.substring(0, 4)
                    + number.substring(5);
        }

        return number;
    }
}