package com.site.toffCo.module.itemcarrinho.repository;

import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, UUID> {
}
