package com.site.toffCo.module.produto.application.command.model;

import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductCommand(
        String name,
        String description,
        BigDecimal price,
        String image,
        List<String> images,
        boolean featured,
        String categoria,
        BigDecimal estoque,
        ProductType type,
        String typePersonalizado,
        String marca,
        Integer peso,
        BigDecimal diametro,
        String codigoBarras,
        ProductStatus status
) {
    /** Compatibilidade com o command anterior, sem os campos novos. */
    public CreateProductCommand(
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
        this(name, description, price, image, List.of(), false, categoria, estoque,
                type, typePersonalizado, marca, peso, diametro, null, status);
    }
}
