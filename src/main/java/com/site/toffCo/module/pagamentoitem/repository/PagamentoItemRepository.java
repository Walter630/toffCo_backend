package com.site.toffCo.module.pagamentoitem.repository;

import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface PagamentoItemRepository extends JpaRepository<PagamentoItem, UUID> {
}
