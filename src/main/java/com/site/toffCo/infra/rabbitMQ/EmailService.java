package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import jakarta.mail.MessagingException;
import jakarta.validation.MessageInterpolator;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

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

    public void sendTemplateEmail(String to, String subject, String template) throws MessagingException {
        var mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setFrom("toffco@gmail.com");
        helper.setSubject(subject);
        helper.setText(template, true);
        mailSender.send(mimeMessage);
    }

    public void sendLoginEmail(String to, String nome) throws MessagingException {
        var mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        Context context =  new Context();
        context.setVariable("nome", nome);

        String htmlBody = templateEngine.process("login.html", context);
        helper.setTo(to);
        helper.setFrom("toffco@gmail.com");
        helper.setSubject("Novo acesso na sua conta ToffCo");
        helper.setText(htmlBody, true);

        mailSender.send(helper.getMimeMessage());
    }
}
