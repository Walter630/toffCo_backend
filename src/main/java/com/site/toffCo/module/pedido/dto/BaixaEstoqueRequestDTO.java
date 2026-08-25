package com.site.toffCo.module.pedido.dto;

import com.site.toffCo.module.pedido.entity.PedidoStatus;

public record BaixaEstoqueRequestDTO(
        PedidoStatus status,
        Boolean baixarEstoque
) {
    public boolean deveBaixarEstoque() {
        return baixarEstoque == null || baixarEstoque;
    }

    public PedidoStatus statusFinal() {
        return status == null ? PedidoStatus.PRONTO : status;
    }
}
