package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
public class PedidoProducer {

    private final AmqpTemplate amqpTemplate;

    public PedidoProducer(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void send(PedidoEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
        System.out.println("Enviando pedido..." + event);
    }
}
