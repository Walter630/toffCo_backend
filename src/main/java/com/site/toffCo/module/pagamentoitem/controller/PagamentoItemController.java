package com.site.toffCo.module.pagamentoitem.controller;

import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.service.PagamentoItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentoitems")
@RequiredArgsConstructor
public class PagamentoItemController {

    private final PagamentoItemService service;

    @PostMapping("/FormaPayment")
    public ResponseEntity<ResponseDTO> processar(@Valid @RequestBody PagamentoRequestDTO requestDTO) {
        ResponseDTO response = service.getPagamentoItem(requestDTO);
        return ResponseEntity.ok(response);
    }
}
