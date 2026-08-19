package com.site.toffCo.module.carrinho.entity;

import com.site.toffCo.module.carrinho.dto.CarrinhoStatus;
import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_carrinho")
public class Carrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(precision =  19, scale = 2)
    private BigDecimal valorTotal;

    @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, orphanRemoval = true, fetch =  FetchType.LAZY)
    private List<ItemCarrinho> itens = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarrinhoStatus carrinhoStatus = CarrinhoStatus.ABERTO;

    private LocalDateTime expiresAt;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version; // O Hibernate usa isso para controle de concorrência

    public ItemCarrinho adicionarOuIncrementarItem(Produto produto, int quantidade) {
        ItemCarrinho itemCarrinho = itens.stream()
                .filter(i -> i.getProduto()
                        .getId()
                        .equals(produto.getId())
                )
                .findFirst()
                .orElse(null);
        if (itemCarrinho != null) {
            itemCarrinho.setQuantidade(
                    itemCarrinho.getQuantidade() + quantidade
            );
            return itemCarrinho;
        }
        ItemCarrinho novoItem = new ItemCarrinho();
        novoItem.setCarrinho(this);
        novoItem.setProduto(produto);
        novoItem.setQuantidade(quantidade);
        novoItem.setPrice(produto.getPrice());

        itens.add(novoItem);

        return novoItem;
    }

    public void removerItemCarrinho(ItemCarrinho itemCarrinho) {
        itens.remove(itemCarrinho);
    }
}
