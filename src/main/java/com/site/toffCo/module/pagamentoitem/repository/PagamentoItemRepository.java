package com.site.toffCo.module.pagamentoitem.repository;

import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoItemRepository extends JpaRepository<PagamentoItem, UUID> {

    // Todos os pagamentos de um pedido (pode haver tentativas múltiplas)
    List<PagamentoItem> findByPedidoIdOrderByDataCriacaoDesc(UUID pedidoId);

    // Pagamento aprovado de um pedido (deve haver no máximo um)
    Optional<PagamentoItem> findByPedidoIdAndStatus(UUID pedidoId, PagamentoStatus status);

    // Quantos pagamentos aprovados no total (para o dashboard)
    long countByStatus(PagamentoStatus status);
}
