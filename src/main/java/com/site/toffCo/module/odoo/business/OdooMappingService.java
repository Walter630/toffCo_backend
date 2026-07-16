package com.site.toffCo.module.odoo.business;

import com.site.toffCo.infra.exception.odoo.OdooBusinessException;
import com.site.toffCo.module.odoo.dto.OdooEventStatus;
import com.site.toffCo.module.odoo.dto.OdooStockWebhookDTO;
import com.site.toffCo.module.odoo.entity.ProcessedOdooEvent;
import com.site.toffCo.module.odoo.repository.ProcessedOdooEventRepository;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdooMappingService {

    private final ProdutoRepository productRepository;
    private final ProcessedOdooEventRepository eventRepository;

    @Transactional
    public void processStockMovement(OdooStockWebhookDTO payload) {
        log.info(
                "Processando movimentação Odoo: id={}, barcode={}, quantity={}, destination={}",
                payload.getId(),
                payload.getProductBarcode(),
                payload.getQuantity(),
                payload.getLocationDestUsage()
        );

        /*
         * Erros de negócio são tratados aqui.
         * Erros técnicos continuam subindo para o RabbitMQ.
         */
        try {
            processBusinessRules(payload);
        } catch (OdooBusinessException exception) {
            log.warn(
                    "Movimentação Odoo rejeitada: id={}, motivo={}",
                    payload.getId(),
                    exception.getMessage()
            );

            saveFailedEvent(payload, exception.getMessage());
        }
    }

    private void processBusinessRules(OdooStockWebhookDTO payload) {
        validatePayload(payload);

        if (eventRepository.existsByOdooMoveLineId(payload.getId())) {
            log.info(
                    "Evento Odoo já processado: id={}",
                    payload.getId()
            );

            return;
        }

        Produto product = productRepository
                .findByCodigoBarras(payload.getProductBarcode())
                .orElseThrow(() -> new OdooBusinessException(
                        "Produto não encontrado pelo código de barras: "
                                + payload.getProductBarcode()
                ));

        BigDecimal estoqueAtual = product.getEstoque() != null
                ? product.getEstoque()
                : BigDecimal.ZERO;

        BigDecimal delta = calculateStockDelta(payload);

        BigDecimal novoEstoque = estoqueAtual.add(delta);

        if (novoEstoque.compareTo(BigDecimal.ZERO) < 0) {
            throw new OdooBusinessException(
                    "Estoque insuficiente. Atual="
                            + estoqueAtual
                            + ", movimentação="
                            + delta
            );
        }

        product.setEstoque(novoEstoque);
        productRepository.save(product);

        eventRepository.save(
                buildEvent(payload, OdooEventStatus.SUCCESS, null)
        );

        log.info(
                "Estoque atualizado: produtoId={}, anterior={}, delta={}, atual={}",
                product.getId(),
                estoqueAtual,
                delta,
                novoEstoque
        );
    }

    private void validatePayload(OdooStockWebhookDTO payload) {
        if (payload == null) {
            throw new OdooBusinessException("Payload não informado");
        }

        if (payload.getId() == null) {
            throw new OdooBusinessException(
                    "ID da movimentação não informado"
            );
        }

        if (payload.getProductBarcode() == null
                || payload.getProductBarcode().isBlank()) {

            throw new OdooBusinessException(
                    "Código de barras não informado"
            );
        }

        if (payload.getQuantity() == null
                || payload.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {

            throw new OdooBusinessException(
                    "Quantidade deve ser maior que zero"
            );
        }

        if (payload.getLocationDestUsage() == null
                || payload.getLocationDestUsage().isBlank()) {

            throw new OdooBusinessException(
                    "Destino da movimentação não informado"
            );
        }
    }

    private BigDecimal calculateStockDelta(
            OdooStockWebhookDTO payload
    ) {
        boolean isSaida = "customer".equalsIgnoreCase(
                payload.getLocationDestUsage()
        );

        return isSaida
                ? payload.getQuantity().negate()
                : payload.getQuantity();
    }

    private void saveFailedEvent(
            OdooStockWebhookDTO payload,
            String errorMessage
    ) {
        /*
         * Pode haver casos em que o ID seja nulo.
         * Confira se a coluna no banco aceita null.
         */
        if (payload == null || payload.getId() == null) {
            log.error(
                    "Não foi possível registrar evento FAILED porque o ID é nulo"
            );

            return;
        }

        if (eventRepository.existsByOdooMoveLineId(payload.getId())) {
            return;
        }

        eventRepository.save(
                buildEvent(payload, OdooEventStatus.FAILED, errorMessage)
        );
    }

    private ProcessedOdooEvent buildEvent(
            OdooStockWebhookDTO payload,
            OdooEventStatus status,
            String errorMessage
    ) {
        ProcessedOdooEvent event = new ProcessedOdooEvent();

        event.setOdooMoveLineId(payload.getId());
        event.setProductBarcode(payload.getProductBarcode());
        event.setStatus(status);
        event.setErrorMessage(errorMessage);
        event.setProcessedAt(LocalDateTime.now());

        return event;
    }
}