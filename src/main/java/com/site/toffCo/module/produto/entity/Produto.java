package com.site.toffCo.module.produto.entity;

import com.site.toffCo.module.odoo.dto.OdooSyncStatus;
import com.site.toffCo.module.produto.dto.Status;
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
@AllArgsConstructor
@Table(name = "tb_produto")
@SQLDelete(sql = "UPDATE tb_produto SET ativo = false WHERE id = ?")
@Filter(name = "filtroProdutoAtivo", condition = "ativo = true")
@SQLRestriction("ativo = true")
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
    private BigDecimal estoque;
    private String type; //PLA, PATG, ACESSORIOS

    @Column(nullable = false)
    private String marca;

    @Column(unique = true)
    private String codigoBarras;

    @Lob
    private byte[] imagemCodigoBarras;

    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;


    //Odoo atributos
    @Column(name = "odoo_product-id", unique = true)
    private Long odooProductId;
/*
    @Column(name = "odoo_product_template_id", unique = true)
    private Long odooProductTemplateId;

    @Column(name = "odoo_sync_status")
    @Enumerated(EnumType.STRING)
    private OdooSyncStatus odooSyncStatus;

    @Column(name = "odoo_last_sync_at")
    private LocalDateTime odooLastSyncAt;

    @Column(name = "odoo_sync_error", columnDefinition = "TEXT")
    private String odooSyncError;

 */
}
