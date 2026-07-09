package com.site.toffCo.module.pagamentoitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoRequestDTO(
        @NotBlank(message = "A forma de pagamento é obrigatória")
        String formaPagamento,
        @NotNull(message = "O valor é obrigatório")
        BigDecimal valor,
        @NotNull(message = "O ID do pedido é obrigatório")
        UUID pedidoId
) {
}
