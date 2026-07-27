package com.site.toffCo.module.produto.application.query;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.presentation.request.ProductQueryFilter;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindAllProductUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final CodigoBarrasServices codigoBarrasServices;
    private final ApplicationEventPublisher publisher;

    //============================== LIST ==============================

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> findAll(ProductQueryFilter filter) {
        Specification<Produto> specification = filter.buildSpecification();
        List<Produto> produtos = repository.findAll(specification);
        List<ProdutoResponseDTO> dtos = mapper.toDto(produtos);
        System.out.println("Qtd de DTOs mapeados para JSON: " + dtos.size());
        if (!dtos.isEmpty()) {
            System.out.println("Primeiro DTO: " + dtos.get(0).name());
        }
        log.info("Produtos encontrados no repository: {}", produtos.size());
        return dtos;
    }
}
