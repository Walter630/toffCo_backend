package com.site.toffCo.module.admin.dto;

import com.site.toffCo.module.carrinho.entity.Carrinho;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Visão do carrinho para o admin.
 * Mostra quem tem itens no carrinho e o que são.
 */
public record AdminCarrinhoDTO(
        UUID carrinhoId,
        UUID userId,
        String userEmail,
        String userName,
        int totalItens,
        BigDecimal valorTotal,
        LocalDateTime ultimaAtualizacao,
        List<AdminItemCarrinhoDTO> itens
) {
    public record AdminItemCarrinhoDTO(
            UUID itemId,
            String nomeProduto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {}

    /** Factory: converte a entidade para o DTO sem expor a entidade fora do service. */
    public static AdminCarrinhoDTO from(Carrinho c) {
        var itensDTO = c.getItens().stream()
                .map(i -> new AdminItemCarrinhoDTO(
                        i.getId(),
                        i.getName(),
                        i.getQuantidade(),
                        i.getPrice(),
                        i.getPrice() != null && i.getQuantidade() != null
                                ? i.getPrice().multiply(BigDecimal.valueOf(i.getQuantidade()))
                                : BigDecimal.ZERO
                ))
                .toList();

        return new AdminCarrinhoDTO(
                c.getId(),
                c.getUser() != null ? c.getUser().getId() : null,
                c.getUser() != null ? c.getUser().getEmail() : "—",
                c.getUser() != null ? c.getUser().getUsername() : "—",
                c.getItens().size(),
                c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO,
                c.getUpdatedAt(),
                itensDTO
        );
    }
}
