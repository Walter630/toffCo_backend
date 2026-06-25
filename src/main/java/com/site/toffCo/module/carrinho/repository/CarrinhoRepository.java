package com.site.toffCo.module.carrinho.repository;

import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarrinhoRepository extends JpaRepository<Carrinho, UUID> {
    Optional<Carrinho> findByUser_Id(UUID id);
}
