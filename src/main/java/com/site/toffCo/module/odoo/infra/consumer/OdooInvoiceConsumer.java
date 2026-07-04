package com.site.toffCo.module.odoo.infra.consumer;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.business.OdooMappingService;
import com.site.toffCo.module.pedido.service.OrderConfirmedOdooEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OdooInvoiceConsumer {

    private final OdooMappingService  odooMappingService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ODOO_INVOICE)
    public void handleInvoiceRequest(OrderConfirmedOdooEvent event) {
        odooMappingService.traduzirEEnviarFatura(event);
    }
}
