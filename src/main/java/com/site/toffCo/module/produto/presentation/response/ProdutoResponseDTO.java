package com.site.toffCo.module.produto.presentation.response;

import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;

import java.math.BigDecimal;
import java.util.UUID;

public record ProdutoResponseDTO(
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
) {}
