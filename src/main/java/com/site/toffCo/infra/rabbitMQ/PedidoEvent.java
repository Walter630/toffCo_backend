package com.site.toffCo.infra.rabbitMQ;

import java.util.List;
import java.util.UUID;

public record PedidoEvent(
        UUID pedidoId,
        UUID usuarioId,
        List<UUID> produtos,
        Float total
) {
}
