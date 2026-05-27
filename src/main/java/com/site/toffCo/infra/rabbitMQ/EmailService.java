package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {
    private MailSender mailSender;
    //private final TemplateEngine

    public void sendEmail(String to, PedidoEvent pedidoEvent) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(to);
        simpleMailMessage.setFrom("toffco@gmail.com.br");
        simpleMailMessage.setSubject("Pedido confirmado!! ");
        simpleMailMessage.setText("""
                Olá! Seu pedido foi confirmado.
                
                                ID do pedido: %s
                                Total: R$ %.2f
                
                                Obrigado por comprar na ToffCo!
                """.formatted(pedidoEvent.pedidoId(), pedidoEvent.total()));
        mailSender.send(simpleMailMessage);
    }

    //public void sendTemplateEmail(String to, String subject, String template, Map<String, Object> model) {
      //  var mimeMessage = mailSender.createMimeMessage();
        //var mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
    //}
}
