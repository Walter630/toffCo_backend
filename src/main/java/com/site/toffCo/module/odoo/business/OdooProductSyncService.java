package com.site.toffCo.module.odoo.business;

import com.site.toffCo.module.odoo.client.OdooProductClient;
import com.site.toffCo.module.odoo.dto.OdooProductRequestDTO;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdooProductSyncService {

    private final ProdutoRepository produtoRepository;
    private final OdooProductClient odooProductClient;

    @Transactional
    public Long syncProduct(UUID produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Produto não encontrado: " + produtoId
                ));

        OdooProductRequestDTO request =
                toOdooRequest(produto);

        /*
         * FASE 1:
         *
         * O personagem já possui o ID do outro mundo.
         */
        if (produto.getOdooProductId() != null) {
            Long odooProductId =
                    produto.getOdooProductId();

            log.info(
                    "Produto já vinculado ao Odoo. Atualizando: produtoId={}, odooProductId={}",
                    produto.getId(),
                    odooProductId
            );

            odooProductClient.updateProduct(
                    odooProductId,
                    request
            );

            return odooProductId;
        }

        /*
         * FASE 2:
         *
         * Não tem ID salvo. Procuramos pelo barcode.
         */
        Long existingOdooProductId = odooProductClient
                .findProductByBarcode(
                        produto.getCodigoBarras()
                )
                .orElse(null);

        /*
         * FASE 3:
         *
         * Encontrou um personagem já existente no Odoo.
         */
        if (existingOdooProductId != null) {
            log.info(
                    "Produto encontrado no Odoo pelo barcode: produtoId={}, odooProductId={}, barcode={}",
                    produto.getId(),
                    existingOdooProductId,
                    produto.getCodigoBarras()
            );

            produto.setOdooProductId(
                    existingOdooProductId
            );

            produtoRepository.save(produto);

            odooProductClient.updateProduct(
                    existingOdooProductId,
                    request
            );

            return existingOdooProductId;
        }

        /*
         * FASE 4:
         *
         * Não tem ID e não foi encontrado pelo barcode.
         * Agora pode criar.
         */
        Long createdOdooId =
                odooProductClient.createProduct(request);

        produto.setOdooProductId(createdOdooId);
        produtoRepository.save(produto);

        log.info(
                "Produto criado e vinculado ao Odoo: produtoId={}, odooProductId={}, barcode={}",
                produto.getId(),
                createdOdooId,
                produto.getCodigoBarras()
        );

        return createdOdooId;
    }

    private OdooProductRequestDTO toOdooRequest(
            Produto produto
    ) {
        return new OdooProductRequestDTO(
                produto.getName(),
                produto.getDescription(),
                produto.getCodigoBarras(),
                produto.getPrice(),
                produto.getEstoque()
        );
    }
}