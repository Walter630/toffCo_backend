package com.site.toffCo.module.odoo.dto;

import java.util.List;
import java.util.UUID;

/**
 * Dados necessários para criar uma fatura (NF-e) no Odoo.
 *
 * Esse DTO trafega na fila RabbitMQ [odoo.invoice.create]
 * e é consumido pelo OdooInvoiceConsumer.
 *
 * @param pedidoId      ID do pedido no nosso sistema (para vincular a NotaFiscal)
 * @param customerName  Nome do cliente (usado para localizar/criar parceiro no Odoo)
 * @param customerCpf   CPF do cliente
 * @param customerEmail E-mail do cliente
 * @param items         Lista de itens do pedido
 */
public record OdooInvoiceCreateDTO(
        UUID pedidoId,
        String customerName,
        String customerCpf,
        String customerEmail,
        List<OdooInvoiceLineDTO> items
) {}
