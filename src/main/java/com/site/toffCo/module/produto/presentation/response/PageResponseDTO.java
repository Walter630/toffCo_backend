package com.site.toffCo.module.produto.presentation.response;

import java.util.List;

public record PageResponseDTO<T>(
        List<T> itens,
        int page,
        int size,
        long totalItens,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
