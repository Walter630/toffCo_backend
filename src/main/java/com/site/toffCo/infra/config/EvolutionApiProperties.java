package com.site.toffCo.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agrupa as configurações da Evolution API num record type-safe.
 *
 * Em vez de @Value("${evolution.api.url}") espalhado por vários services,
 * injeta-se este record diretamente. O Spring valida na inicialização se
 * as propriedades existem e se os tipos batem — falha rápido, não em runtime.
 *
 * Habilitado em: ToffCoApplication via @EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "evolution.api")
public record EvolutionApiProperties(
        String url,
        String key,
        String instance
) {}
