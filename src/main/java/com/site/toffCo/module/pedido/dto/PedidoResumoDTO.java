package com.site.toffCo.module.pedido.dto;

import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoResumoDTO(
        UUID id,
        PedidoStatus status,
        BigDecimal valorTotal,
        String formaPagamento,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        LocalDateTime dataPayment,
        List<ItemDTO> itens
) {

    public record ItemDTO(
            UUID id,
            UUID produtoId,
            String nomeProduto,
            int quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {}

    public static PedidoResumoDTO from(Pedido pedido) {
        List<ItemDTO> itens = pedido.getItens().stream()
                .map(item -> new ItemDTO(
                        item.getId(),
                        item.getProduto().getId(),
                        item.getProduto().getName(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))
                ))
                .toList();

        return new PedidoResumoDTO(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getTotal(),
                pedido.getFormaPagamento(),
                pedido.getDataCriacao(),
                pedido.getDataAtualizacao(),
                pedido.getDataPayment(),
                itens
        );
    }
}
