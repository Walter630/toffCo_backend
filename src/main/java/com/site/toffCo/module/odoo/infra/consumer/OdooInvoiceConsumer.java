package com.site.toffCo.module.odoo.infra.consumer;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.business.OdooInvoiceService;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumer responsável por processar os eventos de emissão de NF-e.
 *
 * Lê a fila [odoo.invoice.create] que é publicada pelo PedidoService
 * durante o checkout.
 *
 * O RabbitMQ cuida do retry automaticamente (configurado no application.yaml):
 *   - max-retries: 3
 *   - initial-interval: 1000ms
 *   - multiplier: 2 (1s, 2s, 4s)
 *
 * Após esgotar os retries, a mensagem vai para a DLQ (se configurada),
 * onde pode ser inspecionada e reprocessada manualmente.
 *
 * Padrão idêntico ao OdooStockConsumer e OdooProductSyncConsumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OdooInvoiceConsumer {

    private final OdooInvoiceService odooInvoiceService;

    @Value("${odoo.invoice.enabled:false}")
    private boolean invoiceEnabled;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ODOO_INVOICE)
    public void consume(OdooInvoiceCreateDTO dto) {
        if (!invoiceEnabled) {
            log.info(
                    "Emissão de NF-e desabilitada (odoo.invoice.enabled=false). " +
                    "Ignorando pedidoId={}",
                    dto.pedidoId()
            );
            return;
        }

        log.info(
                "Mensagem de NF-e recebida da fila: pedidoId={}, cliente={}",
                dto.pedidoId(),
                dto.customerName()
        );

        odooInvoiceService.emitir(dto);

        log.info(
                "Processamento de NF-e finalizado: pedidoId={}",
                dto.pedidoId()
        );
    }
}
