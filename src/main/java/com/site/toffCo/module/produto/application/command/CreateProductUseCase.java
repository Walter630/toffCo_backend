package com.site.toffCo.module.produto.application.command;

import com.google.zxing.WriterException;
import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.infrastructure.messaging.ProductEventPublisher;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ProductEventPublisher eventPublisher;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProdutoResponseDTO create(CreateProductCommand command) {
        Produto produto = mapper.toEntity(command);

        validarCamposObrigatorios(command, produto);
        produto.updatePrice(command.price());
        produto.updateEstoque(command.estoque());
        produto.setStatus(command.status() == null ? ProductStatus.DISPONIVEL : command.status());
        produto.setCodigoBarras(Produto.normalizarCodigoBarras(command.codigoBarras()));

        // O ID é necessário apenas quando o código automático precisa ser gerado.
        produto = repository.save(produto);

        String codigo = produto.getCodigoBarras();
        if (codigo == null) {
            codigo = codigoBarrasServices.gerarCodigoEAN13(produto.getId());
            produto.setCodigoBarras(codigo);
        }

        try {
            produto.setImagemCodigoBarras(
                    codigoBarrasServices.gerarImagemCodigoBarras(codigo)
            );
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException(
                    "Erro ao gerar imagem do código de barras",
                    exception
            );
        }

        Produto produtoSalvo = repository.save(produto);
        eventPublisher.publishUpdate(produtoSalvo);

        log.info(
                "Produto criado: id={}, codigoBarras={}",
                produtoSalvo.getId(),
                produtoSalvo.getCodigoBarras()
        );
        return mapper.toDto(produtoSalvo);
    }

    private void validarCamposObrigatorios(
            CreateProductCommand command,
            Produto produto
    ) {
        if (command == null || produto == null) {
            throw new IllegalArgumentException("Dados do produto são obrigatórios");
        }
        if (command.price() == null || command.estoque() == null) {
            throw new IllegalArgumentException("Preço e estoque são obrigatórios");
        }
        if (command.type() == null) {
            throw new IllegalArgumentException("Tipo do produto é obrigatório");
        }
        if (command.marca() == null || command.marca().isBlank()) {
            throw new IllegalArgumentException("Marca do produto é obrigatória");
        }
    }
}
