package com.site.toffCo.module.odoo.dto;

import java.math.BigDecimal;

/**
 * Representa uma linha da fatura no Odoo (account.move.line).
 *
 * O Odoo espera cada item como um comando de criação dentro do campo
 * "invoice_line_ids" usando o formato de tupla:
 *
 *   [0, 0, { campos do item }]
 *
 * Essa tupla é montada pelo OdooInvoiceClient antes do envio.
 * Aqui guardamos apenas os dados limpos.
 *
 * @param name        Descrição do produto na linha da nota
 * @param quantity    Quantidade
 * @param priceUnit   Preço unitário
 */
public record OdooInvoiceLineDTO(
        String name,
        int quantity,
        BigDecimal priceUnit
) {}
