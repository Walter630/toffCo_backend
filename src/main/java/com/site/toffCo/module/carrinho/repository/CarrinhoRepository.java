package com.site.toffCo.module.carrinho.repository;

import com.site.toffCo.module.admin.dto.AdminCarrinhoDTO;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarrinhoRepository extends JpaRepository<Carrinho, UUID> {

    Optional<Carrinho> findByUser_Id(UUID id);

    @Query("""
        SELECT distinct c
        from Carrinho c
        left join fetch c.itens i
        left join fetch i.produto
        where c.user.id = :userId
        """)
    Optional<Carrinho> findCarrinhoCompletoUserById(UUID userId);

    List<Carrinho> findByExpiresAtBefore(LocalDateTime expiresAt);

    // Admin: carrinhos com pelo menos 1 item (abandonados ou ativos)
    @Query("SELECT c FROM Carrinho c WHERE SIZE(c.itens) > 0 ORDER BY c.updatedAt DESC")
    List<Carrinho> findCarrinhosComItens();

    @Query("SELECT c FROM Carrinho c WHERE exists(select 1 from ItemCarrinho i where i.carrinho = c) order by c.updatedAt desc")
    Page<Carrinho> findByCarrinhosComItem(Pageable pageable);
    // Admin: total de carrinhos com itens
    @Query("SELECT COUNT(c) FROM Carrinho c WHERE SIZE(c.itens) > 0")
    long countCarrinhosComItens();
}
