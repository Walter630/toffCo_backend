package com.site.toffCo.module.odoo.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductChangedEvent(
        UUID productId,
        String name,
        String description,
        String barcode,
        BigDecimal price,
        BigDecimal stock
) {
}
