package com.site.toffCo.module.itemcarrinho.entity;

import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.produto.domain.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_itemcarrinho")
public class ItemCarrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrinho_id", referencedColumnName = "id")
    private Carrinho carrinho;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", referencedColumnName = "id")
    private Produto produto;
    private Integer quantidade;
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
}
