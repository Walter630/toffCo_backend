package com.site.toffCo.infra.rabbitMQ;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private MailSender mailSender;

    public EmailService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, PedidoEvent pedidoEvent) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(to);
        simpleMailMessage.setSubject("Pedido confirmado!! ");
        simpleMailMessage.setText("""
                Olá! Seu pedido foi confirmado.
                
                                ID do pedido: %s
                                Total: R$ %.2f
                
                                Obrigado por comprar na ToffCo!
                """.formatted(pedidoEvent.pedidoId(), pedidoEvent.total()));
        mailSender.send(simpleMailMessage);
    }
}
