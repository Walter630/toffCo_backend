package com.site.toffCo.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "whatsapp.meta")
public record MetaWhatsappProperties(
        boolean enabled,
        String graphUrl,
        String graphVersion,
        String phoneNumberId,
        String accessToken,
        String verifyToken,
        String appSecret,
        Duration readTimeout
) {

    public String apiBaseUrl() {
        String root = graphUrl == null || graphUrl.isBlank()
                ? "https://graph.facebook.com"
                : graphUrl.replaceAll("/+$", "");
        String version = graphVersion == null ? "" : graphVersion.trim();

        if (version.isBlank()) {
            return root;
        }

        return root + "/" + version.replaceAll("^/+|/+$", "");
    }

    public boolean isSendConfigured() {
        return enabled
                && notBlank(phoneNumberId)
                && notBlank(accessToken);
    }

    public boolean isWebhookConfigured() {
        return enabled
                && notBlank(verifyToken)
                && notBlank(appSecret);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
