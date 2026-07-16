package com.site.toffCo.module.odoo.infra.consumer;

import com.site.toffCo.module.odoo.business.OdooMappingService;
import com.site.toffCo.module.odoo.dto.OdooStockWebhookDTO;
import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdooStockConsumer {

    private final OdooMappingService odooMappingService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ODOO_STOCK)
    public void handleStockUpdate(OdooStockWebhookDTO payload) {
        log.info("Message received for Odoo stock update:  id={}", payload.getId());
        odooMappingService.processStockMovement(payload);
        log.info("Message odoo finalized: id={}", payload.getId());
    }
}