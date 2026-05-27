package com.site.toffCo.module.pedido.entity;

import com.site.toffCo.module.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private User user;

    private BigDecimal total; // Use BigDecimal, nunca Float!

    private LocalDateTime dataCriacao;

    // Você precisará de uma lista de itens do pedido (ItemPedido)
    // que guarde o preço unitário de cada produto naquele momento.

}
