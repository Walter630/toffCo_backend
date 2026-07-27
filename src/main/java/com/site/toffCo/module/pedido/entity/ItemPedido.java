package com.site.toffCo.module.pedido.entity;

import com.site.toffCo.module.produto.domain.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot do item no momento em que o pedido foi feito.
 *
 * Por que não reusar ItemCarrinho?
 *   O carrinho é mutável — o cliente pode mudar quantidade ou remover itens.
 *   O pedido precisa de um registro imutável do que foi comprado e por qual
 *   preço, mesmo que o produto mude de preço ou seja removido depois.
 *
 *   Ex: produto custava R$50 hoje. Daqui a 1 mês custa R$70.
 *   O pedido histórico ainda precisa mostrar R$50.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── RELACIONAMENTOS ──────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Referência ao produto original — pode ser null se o produto for excluído
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    // ─── SNAPSHOT ─────────────────────────────────────────────────
    // Dados copiados do carrinho no momento do checkout — imutáveis após criação

    @Column(nullable = false)
    private String nomeProduto;      // cópia do nome no momento da compra

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario; // preço no momento da compra

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;      // quantidade × precoUnitario
}
