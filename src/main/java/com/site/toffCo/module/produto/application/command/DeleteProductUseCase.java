package com.site.toffCo.module.produto.application.command;

import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProdutoRepository repository;

    //============================== DELETE ==============================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            log.error("Produto nao encontrado");
            throw new ProductNotFound("Product not found with id " + id);
        }
        repository.deleteById(id);
        log.info("Produto deletado: {}", id);
    }

}
