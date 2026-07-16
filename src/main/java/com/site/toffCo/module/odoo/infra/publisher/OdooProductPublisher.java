package com.site.toffCo.module.odoo.infra.publisher;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.dto.OdooProductSyncEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdooProductPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OdooProductSyncEventDTO event) {
        log.info(
                "Publicando produto para sincronização com Odoo: productId={}, barcode={}",
                event.productId(),
                event.barcode()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_PRODUCT_SYNC,
                event
        );
    }
}
