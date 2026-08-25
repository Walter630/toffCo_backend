package com.site.toffCo.module.produto.presentation.response;

import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProdutoResponseDTO(
        UUID id,
        String name,
        String description,
        String image,
        List<String> images,
        boolean featured,
        String categoria,
        String marca,
        ProductType type,
        String typePersonalizado,
        BigDecimal price,
        BigDecimal estoque,
        Integer peso,
        BigDecimal diametro,
        String codigoBarras,
        ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** Compatibilidade com o formato interno anterior. */
    public ProdutoResponseDTO(
            UUID id,
            String name,
            String description,
            String image,
            String categoria,
            BigDecimal price,
            BigDecimal estoque,
            ProductType type,
            Integer peso,
            BigDecimal diametro,
            String typePersonalizado,
            ProductStatus status,
            String codigoBarras,
            String marca
    ) {
        this(id, name, description, image, List.of(), false, categoria, marca, type,
                typePersonalizado, price, estoque, peso, diametro, codigoBarras,
                status, null, null);
    }
}
