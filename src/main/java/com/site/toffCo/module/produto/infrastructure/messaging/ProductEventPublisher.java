package com.site.toffCo.module.produto.infrastructure.messaging;

import com.site.toffCo.module.odoo.event.ProductChangedEvent;
import com.site.toffCo.module.produto.domain.Produto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishUpdate(Produto produto) {
        ProductChangedEvent event = new ProductChangedEvent(
                produto.getId(),
                produto.getName(),
                produto.getDescription(),
                produto.getCodigoBarras(),
                produto.getPrice(),
                produto.getEstoque()
        );

        publisher.publishEvent(event);

        log.info(
                "Evento de alteração de produto publicado: productId={}, barcode={}",
                produto.getId(),
                produto.getCodigoBarras()
        );
    }
}
