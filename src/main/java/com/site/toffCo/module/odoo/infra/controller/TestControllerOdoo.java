package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.module.pedido.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestControllerOdoo {

    private final PedidoService pedidoService;

    @GetMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestParam String userId) {
        pedidoService.realizarCheckout(userId);
        return ResponseEntity.ok("Fluxo disparado com sucesso!!");
    }
}
