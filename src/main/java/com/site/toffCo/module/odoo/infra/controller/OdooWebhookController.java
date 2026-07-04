package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.dto.OdooStockWebhookPayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integracao/odoo")
public class OdooWebhookController {

    private final RabbitTemplate rabbitTemplate;

    public OdooWebhookController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/estoque-presencial")
    public ResponseEntity<Void> receiveOdooStockUpdate(@RequestBody OdooStockWebhookPayload payload) {
        // Posta na fila de estoque para processamento em background
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_STOCK,
                payload
        );

        // Responde imediatamente pro Odoo para liberar a requisição externa
        return ResponseEntity.ok().build();
    }
}