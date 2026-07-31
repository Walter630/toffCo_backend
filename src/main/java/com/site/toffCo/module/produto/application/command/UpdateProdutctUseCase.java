package com.site.toffCo.module.produto.application.command;

import com.google.zxing.WriterException;
import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.infrastructure.messaging.ProductEventPublisher;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateProdutctUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final CodigoBarrasServices codigoBarrasServices;
    private final ProductEventPublisher eventPublisher;

    //============================== UPDATE ==============================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProdutoResponseDTO update(
            UUID id,
            UpdateProductCommand command
    ) {
        Produto produto = repository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFound(
                                "Produto não encontrado com id " + id
                        )
                );

        mapper.toUpdateEntity(command, produto);

        /*
         * Caso o update permita remover o código de barras,
         * geramos outro código.
         */
        if (produto.getCodigoBarras() == null
                || produto.getCodigoBarras().isBlank()) {

            String codigo =
                    codigoBarrasServices.gerarCodigoEAN13(produto.getId());

            produto.setCodigoBarras(codigo);

            try {
                produto.setImagemCodigoBarras(
                        codigoBarrasServices
                                .gerarImagemCodigoBarras(codigo)
                );
            } catch (WriterException | IOException exception) {
                throw new RuntimeException(
                        "Erro ao gerar imagem do código de barras",
                        exception
                );
            }
        }

        Produto produtoSalvo = repository.save(produto);

        eventPublisher.publishUpdate(produtoSalvo);

        log.info(
                "Produto atualizado: id={}, barcode={}",
                produtoSalvo.getId(),
                produtoSalvo.getCodigoBarras()
        );

        return mapper.toDto(produtoSalvo);
    }

}
