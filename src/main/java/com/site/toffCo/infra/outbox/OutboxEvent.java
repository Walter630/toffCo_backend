package com.site.toffCo.infra.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

//Ao invés de mandar direto pro RabbitMQ...
     //   → salva na tabela tb_outbox_event junto com o pedido (mesma transação)
//→ um scheduler lê os eventos não publicados
//→ publica no Rabbit
//→ marca como publicado


@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tb_outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Qual pedido gerou o evento
    @Column(nullable = false)
    private UUID aggregateId;

    // tipo do evento gerado PEDIDO_EMAIl
    @Column(nullable = false, length = 50)
    private String typeEvent;

    // o corpo recebido em JSON o payload que o rabbit vai receber
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // controle de comunicação
    @Column(nullable = false)
    private boolean published = false;
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
