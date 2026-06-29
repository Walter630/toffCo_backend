package com.site.toffCo.module.pagamentoitem.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoRequestDTO(
        String formaPagamento,
        BigDecimal valor,
        UUID pedidoId
) {
}
