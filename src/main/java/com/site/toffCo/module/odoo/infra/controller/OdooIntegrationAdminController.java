package com.site.toffCo.module.odoo.infra.controller;

import com.site.toffCo.module.odoo.business.OdooIntegrationQueryService;
import com.site.toffCo.module.odoo.dto.OdooEventStatus;
import com.site.toffCo.module.odoo.dto.OdooIntegrationSummaryDTO;
import com.site.toffCo.module.odoo.dto.OdooStockResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/odoo")
@RequiredArgsConstructor
public class OdooIntegrationAdminController {
    private final OdooIntegrationQueryService service;

    @PostMapping("/summary")
    public ResponseEntity<OdooIntegrationSummaryDTO> summary() {
        return ResponseEntity.ok(
                service.getOdooSummary()
        );
    }

    @GetMapping("/events")
    public ResponseEntity<Page<OdooStockResponseDTO>> findAll(@RequestParam(required = false) OdooEventStatus status, Pageable pageable) {
        return ResponseEntity.ok(
                service.findAll(status, pageable)
        );
    }
}
