package com.site.toffCo.module.carrinho.repository;

import com.site.toffCo.module.carrinho.entity.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarrinhoRepository extends JpaRepository<Carrinho, UUID> {

    Optional<Carrinho> findByUser_Id(UUID id);

    // Admin: carrinhos com pelo menos 1 item (abandonados ou ativos)
    @Query("SELECT c FROM Carrinho c WHERE SIZE(c.itens) > 0 ORDER BY c.updatedAt DESC")
    List<Carrinho> findCarrinhosComItens();

    // Admin: total de carrinhos com itens
    @Query("SELECT COUNT(c) FROM Carrinho c WHERE SIZE(c.itens) > 0")
    long countCarrinhosComItens();
}
