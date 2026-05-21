package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.infra.exception.user.UserNotFound;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.repository.UserRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PedidoConsumer {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public PedidoConsumer(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receive(PedidoEvent pedidoEvent) {
        System.out.println("Enviando pedido..." + pedidoEvent);

        User user = userRepository.findById(pedidoEvent.usuarioId())
                .orElseThrow(() -> new UserNotFound("Usuario no encontrado!"));

        // envia email para o usuario
        emailService.sendEmail(pedidoEvent.emailUser(), pedidoEvent);
        System.out.println("E-mail enviado com sucesso!");
    }
}
