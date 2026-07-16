package com.site.toffCo.module.odoo.infra.listener;

import com.site.toffCo.module.odoo.dto.OdooProductSyncEventDTO;
import com.site.toffCo.module.odoo.event.ProductChangedEvent;
import com.site.toffCo.module.odoo.infra.publisher.OdooProductPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductChangedEventListener {

    private final OdooProductPublisher odooProductPublisher;

    @Value("${odoo.sync.enabled:false}")
    private boolean odooSyncEnabled;
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(ProductChangedEvent event) {
        if (!odooSyncEnabled) {
            log.info(
                    "Odoo product sync is disabled produtoId={}",
                    event.productId());
            return;
        }

        log.info(
                "Produto confirmado no banco. Publicando sincronização com Odoo: productId={}",
                event.productId()
        );

        OdooProductSyncEventDTO message =
                new OdooProductSyncEventDTO(
                        event.productId(),
                        event.name(),
                        event.description(),
                        event.barcode(),
                        event.price(),
                        event.stock()
                );

        odooProductPublisher.publish(message);
    }
}
