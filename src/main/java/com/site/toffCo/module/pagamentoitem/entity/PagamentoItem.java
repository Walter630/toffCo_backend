package com.site.toffCo.module.pagamentoitem.entity;

import com.site.toffCo.module.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de uma tentativa de pagamento vinculada a um pedido.
 *
 * Um pedido pode ter mais de um PagamentoItem:
 *   - cliente tentou pagar com PIX, expirou (EXPIRADO)
 *   - cliente tentou cartão, foi recusado (RECUSADO)
 *   - cliente tentou novamente, aprovado (APROVADO)
 *
 * O admin consegue ver o histórico completo de tentativas por pedido.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_pagamentoitem")
public class PagamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── PEDIDO ───────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // ─── VALOR ────────────────────────────────────────────────────
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // ─── FORMA DE PAGAMENTO ───────────────────────────────────────
    // "PIX", "CARTAO_MAQUININHA", "MONEY" — vem do getTipoPagamento() da strategy
    @Column(nullable = false, length = 30)
    private String formaPagamento;

    // ─── STATUS ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoStatus status = PagamentoStatus.AGUARDANDO;

    // ─── DADOS EXTRAS DO PIX ──────────────────────────────────────
    // Nulos quando formaPagamento != PIX
    @Column(columnDefinition = "TEXT")
    private String pixQrCodeBase64;

    @Column(length = 500)
    private String pixCopiaECola;

    // ─── MENSAGEM DE RETORNO ──────────────────────────────────────
    // Ex: "Aprovado na maquininha", "Timeout ao consultar PIX"
    @Column(length = 255)
    private String mensagemRetorno;

    // ─── TIMESTAMPS ───────────────────────────────────────────────
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;
}
