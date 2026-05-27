package com.site.toffCo.module.pedido.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoEvent(
        UUID pedidoId,
        UUID usuarioId,
        BigDecimal total,
        String emailUser
) implements Serializable {
}
