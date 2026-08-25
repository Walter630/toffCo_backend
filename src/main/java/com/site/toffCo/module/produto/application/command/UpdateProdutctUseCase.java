package com.site.toffCo.module.produto.application.command;

import com.google.zxing.WriterException;
import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateProdutctUseCase {

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final CodigoBarrasServices codigoBarrasServices;
    private final ProductEventPublisher eventPublisher;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProdutoResponseDTO update(UUID id, UpdateProductCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados do produto são obrigatórios");
        }

        Produto produto = repository.findById(id)
                .orElseThrow(() -> new ProductNotFound("Produto não encontrado com id " + id));

        String codigoAnterior = produto.getCodigoBarras();
        ProductStatus statusAnterior = produto.getStatus();
        mapper.toUpdateEntity(command, produto);

        if (command.price() == null || command.estoque() == null) {
            throw new IllegalArgumentException("Preço e estoque são obrigatórios");
        }
        if (command.type() == null) {
            throw new IllegalArgumentException("Tipo do produto é obrigatório");
        }
        if (command.marca() == null || command.marca().isBlank()) {
            throw new IllegalArgumentException("Marca do produto é obrigatória");
        }

        produto.updatePrice(command.price());
        produto.updateEstoque(command.estoque());
        produto.setStatus(command.status() == null ? statusAnterior : command.status());

        String codigoSolicitado = Produto.normalizarCodigoBarras(command.codigoBarras());
        if (command.codigoBarras() == null) {
            codigoSolicitado = Produto.normalizarCodigoBarras(codigoAnterior);
        }
        produto.setCodigoBarras(codigoSolicitado);

        if (produto.getCodigoBarras() == null) {
            produto.setCodigoBarras(codigoBarrasServices.gerarCodigoEAN13(produto.getId()));
        }

        if (!produto.getCodigoBarras().equals(codigoAnterior)) {
            try {
                produto.setImagemCodigoBarras(
                        codigoBarrasServices.gerarImagemCodigoBarras(produto.getCodigoBarras())
                );
            } catch (WriterException | IOException exception) {
                throw new IllegalStateException(
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
