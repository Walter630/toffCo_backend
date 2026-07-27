package com.site.toffCo.module.odoo.entity;

import com.site.toffCo.module.odoo.dto.NotaFiscalStatus;
import com.site.toffCo.module.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa o registro de uma Nota Fiscal emitida pelo Odoo.
 *
 * Ciclo de vida:
 *   PENDENTE  → Consumer recebeu o evento e publicou a fatura no Odoo
 *   EMITIDA   → Odoo confirmou a emissão (webhook recebido com status "posted")
 *   AUTORIZADA → SEFAZ autorizou (chave de acesso preenchida)
 *   ERRO      → Algo falhou em qualquer etapa
 *   CANCELADA → Nota cancelada
 */
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "tb_nota_fiscal")
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ─── RELACIONAMENTO ───────────────────────────────────────────────────
    /*
     * Um pedido tem no máximo uma nota fiscal.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    // ─── DADOS DO ODOO ────────────────────────────────────────────────────

    /*
     * ID do account.move criado no Odoo.
     * Preenchido assim que o Consumer cria a fatura no Odoo.
     */
    @Column(name = "odoo_invoice_id")
    private Long odooInvoiceId;

    /*
     * Número da nota fiscal gerado pelo Odoo (ex: "INV/2025/00001").
     */
    @Column(name = "numero_nota", length = 50)
    private String numeroNota;

    /*
     * Chave de acesso de 44 dígitos retornada pela SEFAZ.
     * Preenchida quando o status muda para AUTORIZADA.
     */
    @Column(name = "chave_acesso", length = 50)
    private String chaveAcesso;

    /*
     * URL do PDF DANFE retornada pelo Odoo.
     */
    @Column(name = "url_danfe", length = 500)
    private String urlDanfe;

    /*
     * URL do XML da NF-e retornada pelo Odoo.
     */
    @Column(name = "url_xml", length = 500)
    private String urlXml;

    /*
     * Mensagem de erro — preenchida quando status = ERRO.
     */
    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    // ─── STATUS ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotaFiscalStatus status = NotaFiscalStatus.PENDENTE;

    // ─── TIMESTAMPS ───────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;
}
