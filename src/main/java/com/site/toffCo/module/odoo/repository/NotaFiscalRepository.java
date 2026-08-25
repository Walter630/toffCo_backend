package com.site.toffCo.module.odoo.repository;

import com.site.toffCo.module.odoo.entity.NotaFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, UUID> {

    /**
     * Busca a nota fiscal pelo ID do pedido vinculado.
     * Usado pelo OdooInvoiceService.consultarStatus() e pelo webhook.
     */
    @Query("SELECT n FROM NotaFiscal n WHERE n.pedido.id = :pedidoId")
    Optional<NotaFiscal> findByPedidoId(@Param("pedidoId") UUID pedidoId);

    @Query("SELECT n FROM NotaFiscal n WHERE n.pedido.id = :pedidoId AND n.pedido.user.id = :userId")
    Optional<NotaFiscal> findByPedidoIdAndPedidoUserId(
            @Param("pedidoId") UUID pedidoId,
            @Param("userId") UUID userId
    );

    /**
     * Guarda de idempotência — verifica se já existe nota para o pedido.
     * Evita duplicatas no caso de retry do RabbitMQ.
     */
    @Query("SELECT COUNT(n) > 0 FROM NotaFiscal n WHERE n.pedido.id = :pedidoId")
    boolean existsByPedidoId(@Param("pedidoId") UUID pedidoId);

    /**
     * Busca a nota fiscal pelo ID gerado no Odoo.
     * Usado pelo webhook para localizar qual nota atualizar.
     */
    Optional<NotaFiscal> findByOdooInvoiceId(Long odooInvoiceId);
}
