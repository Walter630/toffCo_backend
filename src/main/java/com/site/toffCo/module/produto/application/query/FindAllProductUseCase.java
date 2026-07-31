package com.site.toffCo.module.produto.application.query;

import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.presentation.request.ProductQueryFilter;
import com.site.toffCo.module.produto.presentation.response.PageResponseDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindAllProductUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;

    //============================== LIST ==============================

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> findAll(
            ProductQueryFilter filter,
            Pageable pageable
    ) {
        Specification<Produto> specification = filter
                .buildSpecification();

        Page<Produto> produtos = repository
                .findAll(specification, pageable);

        Page<ProdutoResponseDTO> responseDTOS = produtos
                .map(mapper::toDto);

        log.info(
                "Produtos encontrados: {}. Página: {} de {}",
                produtos.getTotalElements(),
                produtos.getNumber(),
                produtos.getTotalPages()
        );

        return new PageResponseDTO<>(
                responseDTOS.getContent(),
                responseDTOS.getNumber(),
                responseDTOS.getSize(),
                responseDTOS.getTotalElements(),
                responseDTOS.getTotalPages(),
                responseDTOS.hasNext(),
                responseDTOS.hasPrevious()
        );
    }
}
