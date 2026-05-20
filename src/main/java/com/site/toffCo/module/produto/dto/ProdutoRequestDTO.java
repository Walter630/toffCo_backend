package com.site.toffCo.module.produto.dto;

import jakarta.validation.constraints.NotBlank;

public record ProdutoRequestDTO(
        @NotBlank(message = "Nome is obrigatory")
        String name,
        @NotBlank(message = "Description")
        String description,
        @NotBlank
        Float price,
        String categoria,
        String image,
        Integer estoque
) {
}
