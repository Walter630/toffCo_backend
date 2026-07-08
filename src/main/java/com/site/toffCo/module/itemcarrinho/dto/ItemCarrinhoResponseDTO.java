package com.site.toffCo.module.itemcarrinho.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record ItemCarrinhoResponseDTO(
    UUID id,
    UUID produtoId,
    String description,
    String name,
    BigDecimal price,
    Integer quantidade
) implements Serializable {}
