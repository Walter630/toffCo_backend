package com.site.toffCo.module.carrinho.dto;

import com.site.toffCo.module.itemcarrinho.dto.ItemCarrinhoResponseDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CarrinhoResponseDTO(
    UUID id,
    List<ItemCarrinhoResponseDTO> items,
    BigDecimal valorTotal
) implements Serializable {}
