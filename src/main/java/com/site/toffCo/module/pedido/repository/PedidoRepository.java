package com.site.toffCo.module.pedido.repository;

import com.site.toffCo.module.pedido.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
}
