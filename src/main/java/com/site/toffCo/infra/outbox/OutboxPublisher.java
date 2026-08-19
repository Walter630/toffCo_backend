package com.site.toffCo.infra.outbox;

import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishEventPendents() {
        List<OutboxEvent> events = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent evento : events) {
            try {
                    String routingKey = resolverRoutingKey(evento.getTypeEvent());
                    Class<?> targetType = resolverTargetType(evento.getTypeEvent());

                    // Deserializa o payload para o tipo correto para evitar double-serialization
                    Object payload = objectMapper.readValue(evento.getPayload(), targetType);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE_NAME,
                            routingKey,
                            payload
                    );

                    evento.setPublished(true);
                    evento.setPublishedAt(LocalDateTime.now());

                    log.debug("Outbox publicado: id={}, type={}",
                            evento.getId(), evento.getTypeEvent());

                } catch (Exception e) {
                    evento.setAttempts(evento.getAttempts() + 1);
                    log.warn("Falha ao publicar outbox id={}, tentativa={}",
                            evento.getId(), evento.getAttempts(), e);
                }
        }
    }

    private String resolverRoutingKey(String eventType) {
        return switch (eventType) {
            case "ODOO_INVOICE" -> RabbitMQConfig.ROUTING_ODOO_INVOICE;
            case "PEDIDO_EMAIL" -> "pedido.criado";
            default -> "evento.desconhecido";
        };
    }

    private Class<?> resolverTargetType(String eventType) {
        return switch (eventType) {
            case "ODOO_INVOICE" -> com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO.class;
            default -> Map.class;
        };
    }
}
