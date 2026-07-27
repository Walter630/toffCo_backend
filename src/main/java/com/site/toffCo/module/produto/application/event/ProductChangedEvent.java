package com.site.toffCo.module.produto.application.event;

import com.site.toffCo.module.produto.domain.Produto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductChangedEvent {

    private final ApplicationEventPublisher publisher;

    private void publishProductChangedEvent(Produto produto) {
        com.site.toffCo.module.odoo.event.ProductChangedEvent event = new com.site.toffCo.module.odoo.event.ProductChangedEvent(
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
