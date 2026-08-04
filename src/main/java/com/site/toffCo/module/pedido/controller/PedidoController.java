package com.site.toffCo.module.pedido.controller;

import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.pedido.dto.PedidoCheckoutResponseDTO;
import com.site.toffCo.module.pedido.dto.PedidoResumoDTO;
import com.site.toffCo.module.pedido.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;
    private final AuthUtil authUtil;

    // ------------------ CHECKOUT -----------------

    @PostMapping("/checkout")
    public ResponseEntity<PedidoCheckoutResponseDTO> checkout() {
        var user = authUtil.getUserLogado();
        return ResponseEntity.ok(pedidoService.realizarCheckout(user.getId()));
    }

    // ------------------ LISTAR PEDIDOS -----------------

    @GetMapping
    public ResponseEntity<List<PedidoResumoDTO>> getPedidosResumo() {
        var user = authUtil.getUserLogado();
        return ResponseEntity.ok(pedidoService.getPedidosResumo(user.getId()));
    }
}
