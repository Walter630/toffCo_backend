package com.site.toffCo.module.odoo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OdooStockResponseDTO(
        UUID id,
        Long odooMoveLineId,
        String productBarcode,
        OdooEventStatus status,
        String errorMessage,
        LocalDateTime processedAt
) {
}
