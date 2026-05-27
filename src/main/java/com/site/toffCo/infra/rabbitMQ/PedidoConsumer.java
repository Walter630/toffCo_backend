package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receive(PedidoEvent pedidoEvent) {
        log.info("Pedido recebido: {}",  pedidoEvent);

        // envia email para o usuario
        emailService.sendEmail(pedidoEvent.emailUser(), pedidoEvent);
        log.info("Email enviado com sucesso!: {}", pedidoEvent);
    }
}
