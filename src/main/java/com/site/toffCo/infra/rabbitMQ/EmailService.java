package com.site.toffCo.infra.rabbitMQ;

import com.site.toffCo.module.pedido.dto.PedidoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("pedidoId", pedidoEvent.pedidoId().toString().substring(0, 8).toUpperCase());
            context.setVariable("total", String.format("R$ %.2f", pedidoEvent.total()));

            String htmlBody = templateEngine.process("pedido-confirmado", context);

            helper.setTo(to);
            helper.setFrom(remetente);
            helper.setSubject("Pedido Confirmado - ToffCo");
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("E-mail de pedido enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail HTML de pedido para: {}", to, e);
        }
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
