package com.site.toffCo.module.admin.controller;

import com.site.toffCo.module.admin.dto.AdminCarrinhoDTO;
import com.site.toffCo.module.admin.dto.AdminDashboardSummaryDTO;
import com.site.toffCo.module.admin.dto.AdminPedidoDTO;
import com.site.toffCo.module.admin.dto.AdminUserDTO;
import com.site.toffCo.module.admin.service.AdminDashboardService;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // bloqueia tudo neste controller para não-admin
public class AdminDashboardController {

    private final AdminDashboardService service;

    // ─── GET /api/admin/summary ───────────────────────────────────
    // Retorna os cards do topo: carrinhos ativos, pedidos por status, faturamento
    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    // ─── GET /api/admin/carrinhos ─────────────────────────────────
    // Lista todos os carrinhos com pelo menos 1 item
    @GetMapping("/carrinhos")
    public ResponseEntity<List<AdminCarrinhoDTO>> getCarrinhos() {
        return ResponseEntity.ok(service.getCarrinhosAtivos());
    }

    // ─── GET /api/admin/pedidos ───────────────────────────────────
    // Lista pedidos paginados. Filtra por status se informado.
    // Ex: /api/admin/pedidos?status=PAGO&page=0&size=20
    @GetMapping("/pedidos")
    public ResponseEntity<Page<AdminPedidoDTO>> getPedidos(
            @RequestParam(required = false) PedidoStatus status,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable
    ) {
        return ResponseEntity.ok(service.getPedidos(status, pageable));
    }

    // ─── GET /api/admin/pedidos/{id} ──────────────────────────────
    // Detalhe completo de um pedido, com todos os itens
    @GetMapping("/pedidos/{id}")
    public ResponseEntity<AdminPedidoDTO> getPedidoById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPedidoById(id));
    }

    // ─── GET /api/admin/usuarios/{userId}/pedidos ─────────────────
    // Histórico de pedidos de um cliente específico
    @GetMapping("/usuarios/{userId}/pedidos")
    public ResponseEntity<List<AdminPedidoDTO>> getPedidosByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getPedidosByUser(userId));
    }

    // ─── PATCH /api/admin/pedidos/{id}/status ─────────────────────
    // Admin atualiza o status de um pedido (ex: marcar como ENVIADO)
    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<AdminPedidoDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam PedidoStatus status
    ) {
        return ResponseEntity.ok(service.updateStatusPedido(id, status));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUserDTO>>  getUsers() {
        return ResponseEntity.ok(service.findAllUsers());
    }
}
