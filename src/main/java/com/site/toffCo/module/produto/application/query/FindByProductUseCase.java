package com.site.toffCo.module.produto.application.query;

import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FindByProductUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;

    //============================== FINDBYID ==============================

    @Transactional(readOnly = true)
    public ProdutoResponseDTO findById(UUID id) {
        log.info("Produto encontrado: {}", id);
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new ProductNotFound("Product not found with id " + id)));
    }
}
