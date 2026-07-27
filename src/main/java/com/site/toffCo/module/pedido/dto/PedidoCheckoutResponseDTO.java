package com.site.toffCo.module.pedido.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PedidoCheckoutResponseDTO(
        UUID pedidoId,
        BigDecimal valor
) {
}
