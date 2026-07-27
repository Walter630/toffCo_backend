package com.site.toffCo.module.odoo.dto;

import java.util.UUID;

/**
 * DTO de resposta para o cliente (frontend) consultar o status de uma NF-e.
 *
 * Retornado pelo endpoint GET /api/notas-fiscais/{pedidoId}
 *
 * @param pedidoId      ID do pedido
 * @param status        Status atual da nota no nosso sistema
 * @param numeroNota    Número da nota (ex: "INV/2025/00001") — pode ser null se pendente
 * @param chaveAcesso   Chave de acesso de 44 dígitos — preenchida após autorização SEFAZ
 * @param urlDanfe      URL do DANFE em PDF — preenchida após autorização
 * @param urlXml        URL do XML da NF-e — preenchida após autorização
 * @param mensagemErro  Mensagem de erro — preenchida apenas quando status = ERRO
 */
public record OdooInvoiceStatusDTO(
        UUID pedidoId,
        NotaFiscalStatus status,
        String numeroNota,
        String chaveAcesso,
        String urlDanfe,
        String urlXml,
        String mensagemErro
) {}
