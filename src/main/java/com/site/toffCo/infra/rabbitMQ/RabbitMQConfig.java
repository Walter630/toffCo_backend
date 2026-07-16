package com.site.toffCo.infra.rabbitMQ;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==================== EXCHANGE PRINCIPAL ====================

    public static final String EXCHANGE_NAME = "toffco.exchange";

    // ==================== PRODUTO / PEDIDO ====================

    public static final String QUEUE_NAME = "produto";
    public static final String ROUTING_KEY = "pedido.criado";

    // ==================== LOGIN ====================

    public static final String LOGIN_QUEUE = "login";
    public static final String LOGIN_ROUTING_KEY = "user.login";

    // ==================== ODOO: NOTA FISCAL ====================

    public static final String QUEUE_ODOO_INVOICE =
            "odoo.invoice.create";

    public static final String ROUTING_ODOO_INVOICE =
            "routing.odoo.invoice";

    // ==================== ODOO: ESTOQUE ====================

    public static final String QUEUE_ODOO_STOCK =
            "odoo.stock.update";

    public static final String ROUTING_ODOO_STOCK =
            "routing.odoo.stock";

    public static final String ODOO_STOCK_DLX =
            "odoo.stock.dlx";

    public static final String ODOO_STOCK_DLQ =
            "odoo.stock.dlq";

    public static final String ROUTING_ODOO_STOCK_DLQ =
            "odoo.stock.dead";

    // ==================== ODOO: SINCRONIZAÇÃO DE PRODUTO ====================

    public static final String ODOO_PRODUCT_SYNC_QUEUE =
            "odoo.product.sync.queue";

    public static final String ROUTING_ODOO_PRODUCT_SYNC =
            "odoo.product.sync";

    public static final String ODOO_PRODUCT_SYNC_DLX =
            "odoo.product.sync.dlx";

    public static final String ODOO_PRODUCT_SYNC_DLQ =
            "odoo.product.sync.dlq";

    public static final String ROUTING_ODOO_PRODUCT_SYNC_DLQ =
            "odoo.product.sync.dead";

    // ==================== EXCHANGES ====================

    @Bean
    public DirectExchange produtoExchange() {
        return new DirectExchange(
                EXCHANGE_NAME,
                true,
                false
        );
    }

    @Bean
    public DirectExchange odooStockDeadLetterExchange() {
        return new DirectExchange(
                ODOO_STOCK_DLX,
                true,
                false
        );
    }

    @Bean
    public DirectExchange odooProductSyncDeadLetterExchange() {
        return new DirectExchange(
                ODOO_PRODUCT_SYNC_DLX,
                true,
                false
        );
    }

    // ==================== FILAS NORMAIS ====================

    @Bean
    public Queue produtoQueue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue loginQueue() {
        return QueueBuilder
                .durable(LOGIN_QUEUE)
                .build();
    }

    @Bean
    public Queue odooInvoiceQueue() {
        return QueueBuilder
                .durable(QUEUE_ODOO_INVOICE)
                .build();
    }

    // ==================== FILA DE ESTOQUE ODOO ====================

    @Bean
    public Queue odooStockQueue() {
        return QueueBuilder
                .durable(QUEUE_ODOO_STOCK)
                .deadLetterExchange(ODOO_STOCK_DLX)
                .deadLetterRoutingKey(ROUTING_ODOO_STOCK_DLQ)
                .build();
    }

    @Bean
    public Queue odooStockDeadLetterQueue() {
        return QueueBuilder
                .durable(ODOO_STOCK_DLQ)
                .build();
    }

    // ==================== FILA DE PRODUTO PARA ODOO ====================

    @Bean
    public Queue odooProductSyncQueue() {
        return QueueBuilder
                .durable(ODOO_PRODUCT_SYNC_QUEUE)
                .deadLetterExchange(ODOO_PRODUCT_SYNC_DLX)
                .deadLetterRoutingKey(
                        ROUTING_ODOO_PRODUCT_SYNC_DLQ
                )
                .build();
    }

    @Bean
    public Queue odooProductSyncDeadLetterQueue() {
        return QueueBuilder
                .durable(ODOO_PRODUCT_SYNC_DLQ)
                .build();
    }

    // ==================== BINDINGS NORMAIS ====================

    @Bean
    public Binding produtoBinding(
            @Qualifier("produtoQueue")
            Queue produtoQueue,

            @Qualifier("produtoExchange")
            DirectExchange produtoExchange
    ) {
        return BindingBuilder
                .bind(produtoQueue)
                .to(produtoExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding loginBinding(
            @Qualifier("loginQueue")
            Queue loginQueue,

            @Qualifier("produtoExchange")
            DirectExchange produtoExchange
    ) {
        return BindingBuilder
                .bind(loginQueue)
                .to(produtoExchange)
                .with(LOGIN_ROUTING_KEY);
    }

    @Bean
    public Binding odooInvoiceBinding(
            @Qualifier("odooInvoiceQueue")
            Queue odooInvoiceQueue,

            @Qualifier("produtoExchange")
            DirectExchange produtoExchange
    ) {
        return BindingBuilder
                .bind(odooInvoiceQueue)
                .to(produtoExchange)
                .with(ROUTING_ODOO_INVOICE);
    }

    // ==================== BINDING ESTOQUE ODOO ====================

    @Bean
    public Binding odooStockBinding(
            @Qualifier("odooStockQueue")
            Queue odooStockQueue,

            @Qualifier("produtoExchange")
            DirectExchange produtoExchange
    ) {
        return BindingBuilder
                .bind(odooStockQueue)
                .to(produtoExchange)
                .with(ROUTING_ODOO_STOCK);
    }

    @Bean
    public Binding odooStockDeadLetterBinding(
            @Qualifier("odooStockDeadLetterQueue")
            Queue odooStockDeadLetterQueue,

            @Qualifier("odooStockDeadLetterExchange")
            DirectExchange odooStockDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(odooStockDeadLetterQueue)
                .to(odooStockDeadLetterExchange)
                .with(ROUTING_ODOO_STOCK_DLQ);
    }

    // ==================== BINDING PRODUTO PARA ODOO ====================

    @Bean
    public Binding odooProductSyncBinding(
            @Qualifier("odooProductSyncQueue")
            Queue odooProductSyncQueue,

            @Qualifier("produtoExchange")
            DirectExchange produtoExchange
    ) {
        return BindingBuilder
                .bind(odooProductSyncQueue)
                .to(produtoExchange)
                .with(ROUTING_ODOO_PRODUCT_SYNC);
    }

    @Bean
    public Binding odooProductSyncDeadLetterBinding(
            @Qualifier("odooProductSyncDeadLetterQueue")
            Queue odooProductSyncDeadLetterQueue,

            @Qualifier("odooProductSyncDeadLetterExchange")
            DirectExchange odooProductSyncDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(odooProductSyncDeadLetterQueue)
                .to(odooProductSyncDeadLetterExchange)
                .with(ROUTING_ODOO_PRODUCT_SYNC_DLQ);
    }

    // ==================== JSON ====================

    @Bean
    public MessageConverter produtoMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter produtoMessageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(
                produtoMessageConverter
        );

        return rabbitTemplate;
    }
}