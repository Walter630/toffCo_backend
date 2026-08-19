package com.site.toffCo.module.pedido.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VendaPresencialRequestDTO(
        @NotNull(message = "O ID do produto é obrigatório")
        UUID productId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser pelo menos 1")
        Integer quantity,

        @NotBlank(message = "O nome do cliente é obrigatório")
        String customerName,

        String customerPhone,
        String customerDocument,
        String notes,

        @NotBlank(message = "A forma de pagamento é obrigatória")
        String paymentMethod,

        boolean baixarEstoque,
        boolean confirmarVenda,
        String status
) {
}
