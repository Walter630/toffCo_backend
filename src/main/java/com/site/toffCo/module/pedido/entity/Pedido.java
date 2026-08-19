package com.site.toffCo.module.pedido.entity;

import com.site.toffCo.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── QUEM COMPROU ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "checkout_idempotency_key", nullable = false)
    private String idempotencyKey;

    // ─── VALOR ────────────────────────────────────────────────────
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    private LocalDateTime dataPayment;
    private String formaPagamento;

    // ─── STATUS ───────────────────────────────────────────────────
    /*
     * Começa em AGUARDANDO_PAGAMENTO.
     * Muda para PAGO quando o PagamentoItem confirma o pagamento.
     * O admin pode mover para EM_SEPARACAO, ENVIADO, ENTREGUE ou CANCELADO.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus status = PedidoStatus.AGUARDANDO_PAGAMENTO;

    // ─── ITENS DO PEDIDO ──────────────────────────────────────────
    /*
     * Snapshot dos itens no momento da compra.
     * Guardamos o preço e quantidade aqui (não no carrinho),
     * para que alterações futuras de produto não afetem pedidos antigos.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemPedido> itens = new ArrayList<>();

    // ─── TIMESTAMPS ───────────────────────────────────────────────
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;

    // Version ele serve para o hibernate detectar conflitos no mesmo pedido
    @Version
    private Long version;

}
