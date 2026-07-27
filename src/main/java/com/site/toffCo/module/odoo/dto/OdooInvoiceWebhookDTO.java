package com.site.toffCo.module.odoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Payload recebido via webhook quando o Odoo atualiza o status de uma fatura.
 *
 * O Odoo envia esse payload para /api/webhooks/odoo/invoice-status
 * após a fatura ser confirmada ou após a SEFAZ retornar.
 *
 * Campos esperados do Odoo:
 *   {
 *     "invoice_id":   123,
 *     "invoice_name": "INV/2025/00001",
 *     "state":        "posted",          <- "draft" | "posted" | "cancel"
 *     "nfe_state":    "autorizada",      <- retornado pelo módulo l10n_br_nfe
 *     "access_key":   "35250...44 dígitos",
 *     "pdf_url":      "https://...",
 *     "xml_url":      "https://..."
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class OdooInvoiceWebhookDTO {

    @JsonProperty("invoice_id")
    private Long invoiceId;

    @JsonProperty("invoice_name")
    private String invoiceName;

    /*
     * Estado da fatura no Odoo:
     *   draft     → rascunho
     *   posted    → confirmada / emitida
     *   cancel    → cancelada
     */
    @JsonProperty("state")
    private String state;

    /*
     * Estado específico da NF-e (módulo l10n_br_nfe):
     *   autorizada, denegada, cancelada, erro_autorizacao
     */
    @JsonProperty("nfe_state")
    private String nfeState;

    /*
     * Chave de acesso de 44 dígitos retornada pela SEFAZ.
     */
    @JsonProperty("access_key")
    private String accessKey;

    @JsonProperty("pdf_url")
    private String pdfUrl;

    @JsonProperty("xml_url")
    private String xmlUrl;
}
