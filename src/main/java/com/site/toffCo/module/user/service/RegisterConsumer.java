package com.site.toffCo.module.user.service;

import com.site.toffCo.infra.rabbitMQ.EmailService;
import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.user.dto.RegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterConsumer {
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.LOGIN_QUEUE)
    public void recieve(RegisterEvent event) {
        log.info("recieve login event: {}", event);
        try {
            emailService.sendLoginEmail(event.emailUser(), event.nameUser());
            log.info("recieve login event: {}", event);
        } catch (Exception _) {
            log.error("Error ao enviar email de login: {}", event.emailUser());
        }
    }
}
