package com.site.toffCo.module.odoo.infra.consumer;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.dto.OdooStockWebhookPayload;
import com.site.toffCo.module.pedido.service.StockService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

public class OdooStockConsumer {
    private final StockService stockService;

    public OdooStockConsumer(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ODOO_STOCK)
    public void processStockDecrease(OdooStockWebhookPayload payload) {
        // Invoca o serviço do módulo de produtos para realizar a baixa real no banco local
        stockService.deductStockFromPresencialSale(payload.sku(), payload.quantidadeSaida());
    }
}
