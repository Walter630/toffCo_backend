package com.site.toffCo.module.produto.service;

import com.site.toffCo.infra.exception.product.ProductNotFound;
import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.queryFilter.ProductQueryFilter;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.repository.UserRepository;
import com.site.toffCo.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final UserRepository userService;

    //============================== CREATE PRODUCT ==============================

    @Transactional
    public ProdutoResponseDTO create(ProdutoRequestDTO produtoDTO) {
        Produto produto = mapper.toEntity(produtoDTO);
        log.info("Produto: {}", produto);
        return mapper.toDto(repository.save(produto));
    }

    //============================== UPDATE ==============================

    @Transactional
    public ProdutoResponseDTO update(UUID id, ProdutoRequestDTO produtoDTO) {
        Produto produto = repository.findById(id).orElseThrow(() ->
                new ProductNotFound("Not found with id " + id));
        mapper.toUpdateEntity(produtoDTO, produto);
        log.info("Produto atualizado: {}", produto);
        return mapper.toDto(repository.save(produto));
    }

    //============================== LIST ==============================

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> findAll(ProductQueryFilter filter) {
        Specification<Produto> specification = filter.buildSpecification();
        log.info("Produtos filtrados: {}", specification);
        return repository.findAll(specification)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    //============================== FINDBYID ==============================

    @Transactional(readOnly = true)
    public ProdutoResponseDTO findById(UUID id) {
        log.info("Produto encontrado: {}", id);
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ProductNotFound("Product not found with id " + id)));
    }

    //============================== DELETE ==============================

    @Transactional
    public void deleteById(UUID id) {
        if(!repository.existsById(id)) {
            log.error("Produto nao encontrado");
            throw new ProductNotFound("Product not found with id " + id);
        }
        if (userService.findByRole(Role.ADMIN)) {
            //deleta somente se for admin
            repository.deleteById(id);
            log.info("Produto deletado: {}", id);
        }
    }

}
