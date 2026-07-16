package com.site.toffCo.module.carrinho.controller;

import com.site.toffCo.module.carrinho.dto.CarrinhoRequestDTO;
import com.site.toffCo.module.carrinho.dto.CarrinhoResponseDTO;
import com.site.toffCo.module.carrinho.service.CarrinhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carrinhos")
public class CarrinhoController {

    private final CarrinhoService service;

    //============================== GETMYCAR ==============================

    @GetMapping
    public ResponseEntity<CarrinhoResponseDTO> myCar() {
        return ResponseEntity.ok(service.findByCar());
    }

    //============================== ADD_ITEM_CAR ==============================

    @PostMapping("/item/{produtoId}")
    public ResponseEntity<CarrinhoResponseDTO> carrinho(@PathVariable UUID produtoId, @RequestParam BigDecimal quantidade) {
        return ResponseEntity.ok(service.addItem(produtoId, quantidade));
    }

    //============================== DELETE_ITEM_ID ==============================

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<CarrinhoResponseDTO> removeItem(@PathVariable UUID itemId) {
        service.removerItem(itemId);
        return ResponseEntity.noContent().build();
    }

    //============================== DELETE_ITEM_ID ==============================

    @PutMapping("/item/{produtoId}")
    public ResponseEntity<CarrinhoResponseDTO>  updateItem( @PathVariable UUID produtoId,
                                                            @RequestParam Integer quantidade) {
        var dto = new CarrinhoRequestDTO(produtoId, quantidade);
        return ResponseEntity.ok(service.updateCar(dto));
    }
}
