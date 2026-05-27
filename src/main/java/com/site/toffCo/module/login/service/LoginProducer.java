package com.site.toffCo.module.login.service;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.login.dto.LoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginProducer {

    private final AmqpTemplate amqpTemplate;

    public void send(LoginEvent event) {
        amqpTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
        log.info("Evento de login enviado para: {}", event.emailUser());
    }
}