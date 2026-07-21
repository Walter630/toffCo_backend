package com.site.toffCo.module.admin.dto;

import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Visão do pedido para o admin.
 * Inclui dados do cliente, itens comprados e status atual.
 */
public record AdminPedidoDTO(
        UUID pedidoId,
        UUID userId,
        String userEmail,
        String userName,
        String userPhone,
        BigDecimal total,
        PedidoStatus status,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        List<AdminItemPedidoDTO> itens
) {
    public record AdminItemPedidoDTO(
            UUID itemId,
            String nomeProduto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {}

    /** Factory: converte a entidade para o DTO. */
    public static AdminPedidoDTO from(Pedido p) {
        var itensDTO = p.getItens().stream()
                .map(i -> new AdminItemPedidoDTO(
                        i.getId(),
                        i.getNomeProduto(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getSubtotal()
                ))
                .toList();

        return new AdminPedidoDTO(
                p.getId(),
                p.getUser() != null ? p.getUser().getId() : null,
                p.getUser() != null ? p.getUser().getEmail() : "—",
                p.getUser() != null ? p.getUser().getUsername() : "—",
                p.getUser() != null ? p.getUser().getPhone() : "—",
                p.getTotal(),
                p.getStatus(),
                p.getDataCriacao(),
                p.getDataAtualizacao(),
                itensDTO
        );
    }
}
