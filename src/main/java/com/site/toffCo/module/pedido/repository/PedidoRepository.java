package com.site.toffCo.module.pedido.repository;

import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    // Admin: pedidos paginados por status
    Page<Pedido> findByStatusOrderByDataCriacaoDesc(PedidoStatus status, Pageable pageable);

    // Admin: todos os pedidos paginados (sem filtro de status)
    Page<Pedido> findAllByOrderByDataCriacaoDesc(Pageable pageable);

    // Admin: pedidos de um usuário específico
    List<Pedido> findByUserIdOrderByDataCriacaoDesc(UUID userId);

    // Admin: total de pedidos por status (para o card de resumo)
    long countByStatus(PedidoStatus status);

    // Admin: soma do total de todos os pedidos pagos (faturamento)
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.status = :status")
    BigDecimal sumTotalByStatus(PedidoStatus status);
}
