package com.site.toffCo.module.produto.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converte ProductType para/do banco de forma tolerante a falhas.
 *
 * Se o banco contiver um valor que não existe no enum (ex: "PECAS", "Placas"),
 * retorna ProductType.OUTRO em vez de lançar IllegalArgumentException.
 * Isso evita que produtos com categorias legadas/erradas quebrem toda a listagem.
 */
@Converter(autoApply = false)
public class ProductTypeConverter implements AttributeConverter<ProductType, String> {

    private static final Logger log = LoggerFactory.getLogger(ProductTypeConverter.class);

    @Override
    public String convertToDatabaseColumn(ProductType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ProductType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return ProductType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("Valor de ProductType desconhecido no banco: '{}'. Usando OUTRO como fallback.", dbData);
            return ProductType.OUTRO;
        }
    }
}
