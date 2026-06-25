package com.site.toffCo.module.pagamentoitem.controller;

import com.site.toffCo.module.pagamentoitem.service.PagamentoItemService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentoitems")
public class PagamentoItemController {

    private final PagamentoItemService service;

    public PagamentoItemController(PagamentoItemService service) {
        this.service = service;
    }
}
