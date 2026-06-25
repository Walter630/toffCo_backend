package com.site.toffCo.module.itemcarrinho.controller;

import com.site.toffCo.module.itemcarrinho.service.ItemCarrinhoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itemcarrinhos")
public class ItemCarrinhoController {

    private final ItemCarrinhoService service;

    public ItemCarrinhoController(ItemCarrinhoService service) {
        this.service = service;
    }
}
