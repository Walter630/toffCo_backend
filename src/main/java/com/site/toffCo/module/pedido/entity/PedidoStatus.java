package com.site.toffCo.module.pedido.entity;

/**
 * Ciclo de vida de um pedido:
 *
 *  AGUARDANDO_PAGAMENTO → cliente fez checkout mas ainda não pagou
 *  PAGO                 → pagamento confirmado (PIX ou maquininha)
 *  EM_SEPARACAO         → equipe separando os produtos
 *  ENVIADO              → nota emitida, produto despachado
 *  ENTREGUE             → entrega confirmada
 *  CANCELADO            → cancelado pelo cliente ou pela loja
 */
public enum PedidoStatus {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    PRONTO,
    EM_SEPARACAO,
    ENVIADO,
    ENTREGUE,
    CANCELADO
}
