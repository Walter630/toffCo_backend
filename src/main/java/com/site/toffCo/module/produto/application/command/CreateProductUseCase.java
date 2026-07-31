package com.site.toffCo.module.produto.application.command;

import com.google.zxing.WriterException;
import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.infrastructure.messaging.ProductEventPublisher;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final CodigoBarrasServices codigoBarrasServices;
    private final ProductEventPublisher  eventPublisher;

    //============================== CREATE PRODUCT ==============================

    //@PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProdutoResponseDTO create(CreateProductCommand command) {
        Produto produto = mapper.toEntity(command);

        /*
         * Primeiro save para garantir que o produto possua ID.
         * Esse ID será usado na geração do EAN-13.
         */
        produto = repository.save(produto);

        String codigo = produto.getCodigoBarras();

        if (codigo == null || codigo.isBlank()) {
            codigo = codigoBarrasServices.gerarCodigoEAN13(produto.getId());
            produto.setCodigoBarras(codigo);
        }

        log.info(
                "Código de barras definido: produtoId={}, codigo={}",
                produto.getId(),
                codigo
        );

        try {
            byte[] imagemCodigoBarras =
                    codigoBarrasServices.gerarImagemCodigoBarras(codigo);

            produto.setImagemCodigoBarras(imagemCodigoBarras);
        } catch (WriterException | IOException exception) {
            throw new RuntimeException(
                    "Erro ao gerar imagem do código de barras",
                    exception
            );
        }

        /*
         * Salva o estado completo do produto.
         */
        Produto produtoSalvo = repository.save(produto);

        /*
         * O evento precisa ser criado somente depois que todos os campos
         * estiverem preenchidos.
         */
        eventPublisher.publishUpdate(produtoSalvo);

        log.info(
                "Produto criado: id={}, codigoBarras={}",
                produtoSalvo.getId(),
                produtoSalvo.getCodigoBarras()
        );

        return mapper.toDto(produtoSalvo);
    }
}
