package com.site.toffCo.module.odoo.dto;

/**
 * Ciclo de vida da Nota Fiscal no sistema.
 *
 *  PENDENTE    → evento publicado, aguardando processamento pelo Consumer
 *  EMITIDA     → fatura criada e confirmada no Odoo (status "posted")
 *  AUTORIZADA  → SEFAZ autorizou (chave de acesso recebida via webhook)
 *  ERRO        → falha em qualquer etapa (mensagemErro preenchida na entidade)
 *  CANCELADA   → nota cancelada pelo operador
 */
public enum NotaFiscalStatus {
    PENDENTE,
    EMITIDA,
    AUTORIZADA,
    ERRO,
    CANCELADA
}
