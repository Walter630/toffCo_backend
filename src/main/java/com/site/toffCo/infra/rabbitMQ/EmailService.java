package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String remetente;

    public void sendEmail(String to, PedidoEvent pedidoEvent) {
        log.info(
                "Preparando e-mail de pedido. remetente={}, destinatario={}",
                remetente,
                to
        );
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();

        simpleMailMessage.setTo(to);
        simpleMailMessage.setFrom(remetente);
        simpleMailMessage.setSubject("Pedido confirmado!! ");
        simpleMailMessage.setText("""
                Olá! Seu pedido foi confirmado.
                
                                ID do pedido: %s
                                Total: R$ %.2f
                
                                Obrigado por comprar na ToffCo!
                """.formatted(pedidoEvent.pedidoId(), pedidoEvent.total()));
        mailSender.send(simpleMailMessage);
        log.info("E-mail de pedido enviado para: {}", to);
    }

    public void sendTemplateEmail(String to, String subject, String template) throws MessagingException {
        var mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(to);
        helper.setFrom(remetente);
        helper.setSubject(subject);
        helper.setText(template, true);
        log.info(
                "Enviando e-mail HTML. remetente={}, destinatario={}",
                remetente,
                to
        );
        mailSender.send(mimeMessage);
    }

    public void sendLoginEmail(String to, String nome) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        Context context =  new Context();
        context.setVariable("nome", nome);

        String htmlBody = templateEngine.process("test-mail", context);
        helper.setTo(to);
        helper.setFrom(remetente);
        helper.setSubject("Bem-vindo a toffco!");
        helper.setText(htmlBody, true);
        log.info(
                "Enviando e-mail de cadastro. remetente={}, destinatario={}",
                remetente,
                to
        );
        mailSender.send(mimeMessage);
        log.info("E-mail de cadastro enviado para: {}", to);
    }
}
