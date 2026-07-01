package com.site.toffCo.module.produto.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProdutoResponseDTO(
        UUID id,
        String name,
        String description,
        String image,
        String categoria,
        BigDecimal price,
        Integer estoque,
        String type,
        Status status
) {}
