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
import java.util.List;

public record ProdutoRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String name,
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 10000, message = "Descrição deve ter no máximo 10000 caracteres")
        String description,
        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal price,
        String categoria,
        String image,
        List<String> images,
        boolean featured,
        @NotNull(message = "Estoque é obrigatório")
        @PositiveOrZero(message = "Estoque não pode ser negativo")
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
                images == null ? List.of() : List.copyOf(images),
                featured,
                categoria,
                estoque,
                type,
                typePersonalizado,
                marca,
                peso,
                diametro,
                codigoBarras,
                status
        );
    }
}
