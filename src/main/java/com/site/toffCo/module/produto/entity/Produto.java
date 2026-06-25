package com.site.toffCo.module.produto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_produto")
@SQLDelete(sql = "UPDATE tb_produto SET ativo = false WHERE id = ?")
@Filter(name = "filtroProdutoAtivo", condition = "ativo = true")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private boolean ativo = true;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
    private String image;
    private String categoria;
    private Integer estoque;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
