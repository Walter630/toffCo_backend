package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.dto.OdooStockWebhookDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/odoo")
public class OdooWebhookController {

    private final RabbitTemplate rabbitTemplate;

    @Value("${odoo.webhook.token}")
    private String expectedtoken;
    public OdooWebhookController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/stock-update")
    public ResponseEntity<Void> receiveOdooStockUpdate(@RequestParam("token") String token, @RequestBody OdooStockWebhookDTO payload) {
        // Posta na fila de estoque para processamento em background
        log.info(
                "Webhook recebido: id={}, barcode={}, quantity={}, destination={}, reference={}",
                payload.getId(),
                payload.getProductBarcode(),
                payload.getQuantity(),
                payload.getLocationDestUsage(),
                payload.getReference()
        );
        if (!expectedtoken.equals(token)) {
            log.warn("Webhook recebido: token={}, error={}", token, expectedtoken);
            return ResponseEntity.status(401).build();
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_STOCK,
                payload
        );

        // Responde imediatamente pro Odoo para liberar a requisição externa
        return ResponseEntity.accepted().build();
    }



}