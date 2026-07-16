package com.site.toffCo.module.odoo.dto;

public record OdooIntegrationSummaryDTO(
        long total,
        long sucess,
        long failed
) {
}
