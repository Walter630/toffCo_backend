package com.site.toffCo.module.admin.dto;

import com.site.toffCo.module.carrinho.entity.Carrinho;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Visão administrativa do carrinho.
 * Mostra o usuário, os produtos e os valores do carrinho.
 */
public record AdminCarrinhoDTO(
        UUID carrinhoId,
        UUID userId,
        String userEmail,
        String userName,
        String userPhone,
        int totalItens,
        BigDecimal valorTotal,
        LocalDateTime ultimaAtualizacao,
        List<AdminItemCarrinhoDTO> itens
) {

    public record AdminItemCarrinhoDTO(
            UUID itemId,
            String nomeProduto,
            Integer quantidade,
            String marcaProduto,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
    }

    public static AdminCarrinhoDTO from(Carrinho carrinho) {
        if (carrinho == null) {
            throw new IllegalArgumentException(
                    "O carrinho não pode ser nulo"
            );
        }

        /*
         * Normalmente a coleção de itens de uma entidade JPA
         * já deve ser inicializada e não ser nula.
         */
        var itensCarrinho = carrinho.getItens();

        var itensDTO = itensCarrinho.stream()
                .map(item -> {
                    var produto = item.getProduto();

                    String nomeProduto = produto != null
                            ? valueOrDefault(produto.getName())
                            : "Produto não identificado";

                    /*
                     * Aqui será retornado "Elegoo", "Creality",
                     * "Toff Brasil" ou qualquer marca do produto.
                     */
                    String marcaProduto = produto != null
                            ? valueOrDefault(produto.getMarca())
                            : "—";

                    BigDecimal precoUnitario = item.getPrice() != null
                            ? item.getPrice()
                            : BigDecimal.ZERO;

                    Integer quantidade = item.getQuantidade();

                    int quantidadeSegura = quantidade != null
                            ? quantidade
                            : 0;

                    BigDecimal subtotal = precoUnitario.multiply(
                            BigDecimal.valueOf(quantidadeSegura)
                    );

                    return new AdminItemCarrinhoDTO(
                            item.getId(),
                            nomeProduto,
                            quantidadeSegura,
                            marcaProduto,
                            precoUnitario,
                            subtotal
                    );
                })
                .toList();

        int totalItens = itensCarrinho.stream()
                .mapToInt(item -> {
                    Integer quantidade = item.getQuantidade();

                    return quantidade != null
                            ? quantidade
                            : 0;
                })
                .sum();

        return new AdminCarrinhoDTO(
                carrinho.getId(),
                carrinho.getUser() != null
                        ? carrinho.getUser().getId()
                        : null,

                carrinho.getUser() != null
                        ? valueOrDefault(carrinho.getUser().getEmail())
                        : "—",

                carrinho.getUser() != null
                        ? valueOrDefault(carrinho.getUser().getUsername())
                        : "—",

                carrinho.getUser() != null
                        ? valueOrDefault(carrinho.getUser().getPhone())
                        : "—",

                totalItens,

                carrinho.getValorTotal() != null
                        ? carrinho.getValorTotal()
                        : BigDecimal.ZERO,

                carrinho.getUpdatedAt(),

                itensDTO
        );
    }

    private static String valueOrDefault(String value) {
        return value == null || value.isBlank()
                ? "—"
                : value;
    }
}