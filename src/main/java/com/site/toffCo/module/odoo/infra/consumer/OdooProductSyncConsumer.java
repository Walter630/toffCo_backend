package com.site.toffCo.module.odoo.infra.consumer;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.business.OdooProductSyncService;
import com.site.toffCo.module.odoo.dto.OdooProductSyncEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdooProductSyncConsumer {

    private final OdooProductSyncService syncService;

    @RabbitListener(
            queues = RabbitMQConfig.ODOO_PRODUCT_SYNC_QUEUE
    )
    public void consume(OdooProductSyncEventDTO event) {
        log.info(
                "Consumindo sincronização de produto: productId={}, barcode={}",
                event.productId(),
                event.barcode()
        );

        Long odooProductId =
                syncService.syncProduct(
                        event.productId()
                );


        log.info(
                "Produto sincronizado com Odoo: productId={}, odooProductId={}",
                event.productId(),
                odooProductId
        );
    }
}