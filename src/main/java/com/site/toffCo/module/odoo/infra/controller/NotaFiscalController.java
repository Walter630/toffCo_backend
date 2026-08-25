package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.odoo.business.OdooInvoiceService;
import com.site.toffCo.module.odoo.dto.OdooInvoiceStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/nota-fiscal", "/api/notas-fiscais"})
@RequiredArgsConstructor
public class NotaFiscalController {

    private final OdooInvoiceService odooInvoiceService;
    private final AuthUtil authUtil;

    @GetMapping("/{pedidoId}")
    public ResponseEntity<OdooInvoiceStatusDTO> getNotaFiscal(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(odooInvoiceService.consultarStatus(
                pedidoId,
                authUtil.getUserLogado()
        ));
    }
}
