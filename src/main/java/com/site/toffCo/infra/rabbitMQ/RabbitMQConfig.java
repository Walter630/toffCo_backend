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

    public static final String QUEUE_NAME = "produto";
    public static final String EXCHANGE_NAME = "toffco.exchange";
    public static final String ROUTING_KEY = "pedido.criado";

    @Bean
    public Queue produtoQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange produtoExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding produtoBinding() {
        return BindingBuilder
                .bind(produtoQueue())
                .to(produtoExchange())
                .with(ROUTING_KEY);
    }
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

