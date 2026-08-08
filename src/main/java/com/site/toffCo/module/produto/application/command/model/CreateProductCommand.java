package com.site.toffCo.module.produto.application.command.model;

import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;

import java.math.BigDecimal;

public record CreateProductCommand(
        String name,
        String description,
        BigDecimal price,
        String image,
        String categoria,
        BigDecimal estoque,
        ProductType type,
        String typePersonalizado,
        String marca,
        Integer peso,
        BigDecimal diametro,
        ProductStatus status
) {
}
