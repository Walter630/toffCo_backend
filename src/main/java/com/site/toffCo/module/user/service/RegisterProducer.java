package com.site.toffCo.module.user.service;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.user.dto.RegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterProducer {

    private final AmqpTemplate amqpTemplate;

    public void send(RegisterEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.LOGIN_ROUTING_KEY,
                event
        );
        log.info("Evento de login enviado para: {}", event.emailUser());
    }
}