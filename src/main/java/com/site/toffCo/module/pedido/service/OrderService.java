package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.pedido.dto.OrderConfirmedEvent;
import com.site.toffCo.module.pedido.dto.OrderItemEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final RabbitTemplate rabbitTemplate;

    public void completeOrder(Long orderId) {
        var items = List.of(new OrderItemEvent("PROD-123", 2, 49.99));
        var event = new OrderConfirmedEvent(orderId, "123.123.123-32", 99.80, items);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_INVOICE,
                event
        );
    }
}
