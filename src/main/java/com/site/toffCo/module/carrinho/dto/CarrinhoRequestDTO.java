package com.site.toffCo.module.carrinho.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CarrinhoRequestDTO(
        @NotNull(message = "O Id do produto nao pode ser null")
        UUID produtoId,
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 0, message = "A quantidade não pode ser negativa")
        Integer quantidade
) {

}
