package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.business.OdooInvoiceService;
import com.site.toffCo.module.odoo.dto.OdooInvoiceWebhookDTO;
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
    private final OdooInvoiceService odooInvoiceService;

    @Value("${odoo.webhook.token}")
    private String expectedToken;

    public OdooWebhookController(
            RabbitTemplate rabbitTemplate,
            OdooInvoiceService odooInvoiceService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.odooInvoiceService = odooInvoiceService;
    }

    // =========================================================================
    // WEBHOOK DE ESTOQUE (já existia)
    // =========================================================================

    @PostMapping("/stock-update")
    public ResponseEntity<Void> receiveOdooStockUpdate(
            @RequestParam("token") String token,
            @RequestBody OdooStockWebhookDTO payload
    ) {
        log.info(
                "Webhook estoque recebido: id={}, barcode={}, quantity={}, destination={}, reference={}",
                payload.getId(),
                payload.getProductBarcode(),
                payload.getQuantity(),
                payload.getLocationDestUsage(),
                payload.getReference()
        );

        if (!expectedToken.equals(token)) {
            log.warn("Webhook Odoo rejeitado: token inválido");
            return ResponseEntity.status(401).build();
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_STOCK,
                payload
        );

        return ResponseEntity.accepted().build();
    }

    // =========================================================================
    // WEBHOOK DE NOTA FISCAL (novo)
    // =========================================================================

    /**
     * Recebe o retorno do Odoo após a SEFAZ processar a NF-e.
     *
     * O Odoo deve ser configurado para chamar este endpoint quando
     * o estado da fatura mudar (ex: após autorização ou erro da SEFAZ).
     *
     * URL para configurar no Odoo:
     *   POST https://seu-dominio.com/api/webhooks/odoo/invoice-status?token=SEU_TOKEN
     *
     * Processamos de forma síncrona aqui pois a operação é apenas uma
     * atualização de banco — rápida o suficiente para não bloquear o Odoo.
     */
    @PostMapping("/invoice-status")
    public ResponseEntity<Void> receiveInvoiceStatus(
            @RequestParam("token") String token,
            @RequestBody OdooInvoiceWebhookDTO payload
    ) {
        log.info(
                "Webhook NF-e recebido: odooInvoiceId={}, state={}, nfeState={}",
                payload.getInvoiceId(),
                payload.getState(),
                payload.getNfeState()
        );

        if (!expectedToken.equals(token)) {
            log.warn("Webhook NF-e rejeitado: token inválido");
            return ResponseEntity.status(401).build();
        }

        odooInvoiceService.processarWebhook(payload);

        return ResponseEntity.ok().build();
    }


}