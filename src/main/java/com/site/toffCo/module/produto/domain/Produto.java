package com.site.toffCo.module.produto.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "tb_produto")
@SQLDelete(sql = "UPDATE tb_produto SET ativo = false WHERE id = ?")
@SQLRestriction("ativo = true")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private boolean ativo = true;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    private String image;
    private String categoria;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal estoque = BigDecimal.ZERO;

    @Convert(converter = ProductTypeConverter.class)
    @Column(nullable = false)
    private ProductType type;

    private String typePersonalizado;

    @Column(nullable = false)
    private String marca;

    private Integer peso;
    private BigDecimal diametro;

    @Column(unique = true)
    private String codigoBarras;

    @Lob
    private byte[] imagemCodigoBarras;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "odoo_product-id", unique = true)
    private Long odooProductId;

    public void updatePrice(BigDecimal newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    public void updateEstoque(BigDecimal newEstoque) {
        validateEstoque(newEstoque);
        this.estoque = newEstoque;
    }

    public void retirarEstoque(BigDecimal quantidade) {
        validateQuantidade(quantidade);

        if (estoque.compareTo(quantidade) < 0) {
            throw new IllegalStateException(
                    "Estoque insuficiente"
            );
        }

        this.estoque = estoque.subtract(quantidade);
    }

    public void adicionarEstoque(BigDecimal quantidade) {
        validateQuantidade(quantidade);
        this.estoque = estoque.add(quantidade);
    }

    public void desativar() {
        this.ativo = false;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O preço deve ser maior que zero"
            );
        }
    }

    private void validateEstoque(BigDecimal estoque) {
        if (estoque == null || estoque.signum() < 0) {
            throw new IllegalArgumentException(
                    "O estoque não pode ser negativo"
            );
        }
    }

    private void validateQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero"
            );
        }
    }

    public boolean estaSemCodigoBarras() {
        return codigoBarras == null || codigoBarras.isBlank();
    }

    public void definirCodigoBarras(
            String codigoBarras,
            byte[] imagemCodigoBarras
    ) {
        if (codigoBarras == null || codigoBarras.isBlank()) {
            throw new IllegalArgumentException(
                    "O código de barras não pode estar vazio"
            );
        }

        if (imagemCodigoBarras == null || imagemCodigoBarras.length == 0) {
            throw new IllegalArgumentException(
                    "A imagem do código de barras não pode estar vazia"
            );
        }

        this.codigoBarras = codigoBarras;
        this.imagemCodigoBarras = imagemCodigoBarras;
    }
}