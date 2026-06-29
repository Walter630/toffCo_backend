package com.site.toffCo.module.pagamentoitem.dto;

public record ResponseDTO(
         String tipoPagamento, // "PIX" ou "CARTAO_MAQUININHA"
         String status, // "AGUARDANDO_PAGAMENTO", "PAGO", "RECUSADO"

        // Campos exclusivos para o fluxo do PIX (ficam nulos se for cartão)
         String qrCodeBase64,
         String copiaECola,

        // Campo informativo geral (ex: "Aprovado na maquininha", "Erro de timeout")
         String mensagem
) {
}
