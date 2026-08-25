package com.site.toffCo.module.pedido.controller;

import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.pedido.dto.BaixaEstoqueRequestDTO;
import com.site.toffCo.module.pedido.dto.PedidoCheckoutResponseDTO;
import com.site.toffCo.module.pedido.dto.PedidoResumoDTO;
import com.site.toffCo.module.pedido.dto.VendaPresencialRequestDTO;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final AuthUtil authUtil;

    // ------------------ CHECKOUT (via carrinho) -----------------

    @PostMapping("/checkout")
    public ResponseEntity<PedidoCheckoutResponseDTO> checkout(@RequestHeader("Idempotency-Key") String idempotencyKey) {
        var user = authUtil.getUserLogado();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key é obrigatória");
        }
        return ResponseEntity.ok(pedidoService.realizarCheckout(user.getId(), idempotencyKey));
    }

    // ------------------ VENDA PRESENCIAL (balcão) -----------------

    @PostMapping
    public ResponseEntity<PedidoCheckoutResponseDTO> vendaPresencial(
            @Valid @RequestBody VendaPresencialRequestDTO request
    ) {
        var user = authUtil.getUserLogado();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.criarVendaPresencial(user.getId(), request));
    }

    // ------------------ LISTAR PEDIDOS -----------------

    @GetMapping
    public ResponseEntity<List<PedidoResumoDTO>> getPedidosResumo() {
        var user = authUtil.getUserLogado();
        return ResponseEntity.ok(pedidoService.getPedidosResumo(user.getId()));
    }

    // ------------------ BUSCAR PEDIDO POR ID -----------------

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResumoDTO> getPedidoById(@PathVariable UUID id) {
        var user = authUtil.getUserLogado();
        return ResponseEntity.ok(pedidoService.getPedidoById(id, user.getId()));
    }

    // ------------------ AÇÕES ADMIN NO PEDIDO -----------------

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/baixar-estoque")
    public ResponseEntity<PedidoResumoDTO> baixarEstoque(
            @PathVariable UUID id,
            @RequestBody(required = false) BaixaEstoqueRequestDTO request
    ) {
        return ResponseEntity.ok(pedidoService.baixarEstoque(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResumoDTO> confirmarPedido(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.confirmarPagamento(id));
    }
}
