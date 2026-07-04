package com.site.toffCo.infra.rabbitMQ;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange principal do sistema
    public static final String QUEUE_NAME = "produto";

    // Fluxo de Produto / Pedido local
    public static final String EXCHANGE_NAME = "toffco.exchange";
    public static final String ROUTING_KEY = "pedido.criado";

    // Fluxo de Login
    public static final String LOGIN_QUEUE = "login";
    public static final String LOGIN_ROUTING_KEY = "user.login";

    // Fluxo do Odoo (1. Sistema -> Odoo: Emissao de notas)
    public static final String QUEUE_ODOO_INVOICE = "odoo.invoice.create";
    public static final String ROUTING_ODOO_INVOICE = "routing.odoo.invoice";

    // Fluxo do Odoo (2. Odoo -> Sistema: Atualizaçao do estoque via webhook)
    public static final String QUEUE_ODOO_STOCK = "odoo.stock.update";
    public static final String ROUTING_ODOO_STOCK = "routing.odoo.stock";

    // --- Definiçao das filas

    @Bean
    public Queue produtoQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Queue loginQueue() {
        return new Queue(LOGIN_QUEUE, true);
    }

    @Bean
    public Queue odooInvoiceQueue() {
        return new Queue(QUEUE_ODOO_INVOICE, true);
    }

    @Bean
    public Queue odooStockQueue() {
        return new Queue(QUEUE_ODOO_STOCK, true);
    }

    // -- Definicao da Exchange --
    @Bean
    public DirectExchange produtoExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // -- Bindings (Amarraçoes) --

    @Bean
    public Binding produtoBinding() {
        return BindingBuilder
                .bind(produtoQueue())
                .to(produtoExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding loginBinding() {
        return BindingBuilder
                .bind(loginQueue())
                .to(produtoExchange())
                .with(LOGIN_ROUTING_KEY);
    }

    @Bean
    public Binding odooInvoiceBinding() {
        return BindingBuilder
                .bind(odooInvoiceQueue())
                .to(produtoExchange())
                .with(ROUTING_ODOO_INVOICE);
    }

    @Bean
    public Binding odooStockBinding() {
        return BindingBuilder
                .bind(odooStockQueue())
                .to(produtoExchange())
                .with(ROUTING_ODOO_STOCK);
    }

    // --- Configurações de Serialização ---

    @Bean
    public MessageConverter produtoMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(produtoMessageConverter());
        return rabbitTemplate;
    }
}

