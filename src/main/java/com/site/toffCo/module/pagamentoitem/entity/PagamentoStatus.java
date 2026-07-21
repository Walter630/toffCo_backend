package com.site.toffCo.module.pagamentoitem.entity;

/**
 * Status de um registro de pagamento:
 *
 *  AGUARDANDO  → gerado mas não confirmado ainda (ex: PIX gerado, cliente não pagou)
 *  APROVADO    → pagamento confirmado com sucesso
 *  RECUSADO    → recusado pela operadora (cartão sem limite, etc)
 *  EXPIRADO    → PIX ou tentativa que venceu sem pagamento
 *  ESTORNADO   → pagamento aprovado e depois revertido
 */
public enum PagamentoStatus {
    AGUARDANDO,
    APROVADO,
    RECUSADO,
    EXPIRADO,
    ESTORNADO
}
