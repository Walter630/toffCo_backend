package com.site.toffCo.module.odoo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OdooProductSyncEventDTO(
        UUID productId,
        String name,
        String description,
        String barcode,
        BigDecimal price,
        BigDecimal stock
) {
}
