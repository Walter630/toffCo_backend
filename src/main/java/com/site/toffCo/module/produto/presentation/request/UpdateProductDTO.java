package com.site.toffCo.module.produto.presentation.request;

import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductDTO(

        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 10000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(
                value = "0.01",
                message = "Preço deve ser maior que zero"
        )
        BigDecimal price,

        String categoria,

        String image,

        @NotNull(message = "Estoque é obrigatório")
        @PositiveOrZero(message = "Estoque não pode ser negativo")
        BigDecimal estoque,

        ProductType type,

        String typePersonalizado,

        String marca,

        Integer peso,

        BigDecimal diametro,

        ProductStatus status
) {

    public UpdateProductCommand toCommand() {
        return new UpdateProductCommand(
                name,
                description,
                price,
                image,
                categoria,
                estoque,
                type,
                typePersonalizado,
                marca,
                peso,
                diametro,
                status
        );
    }
}