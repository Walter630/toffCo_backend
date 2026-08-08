package com.site.toffCo.module.produto.presentation.request;

import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequestDTO(
        @NotBlank(message = "Nome is obrigatory")
        String name,
        @NotBlank(message = "Description")
        @Size(max = 10000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,
        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal price,
        String categoria,
        String image,
        @NotNull(message = "Estoque é obrigatorio")
        @PositiveOrZero(message = "Estoque nao pode ser negativo")
        BigDecimal estoque,
        String codigoBarras,
        ProductType type,
        String typePersonalizado,
        String marca,
        Integer peso,
        BigDecimal diametro,
        ProductStatus status
) {

        public CreateProductCommand toCommand() {
                return new CreateProductCommand(
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
