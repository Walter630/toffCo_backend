package com.site.toffCo.infra.rabbitMQ;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoEvent(
        UUID pedidoId,
        UUID usuarioId,
        List<UUID> produtos,
        BigDecimal total,
        String emailUser
) implements Serializable {
}
