package com.site.toffCo.infra.exception.stock;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientStock extends RuntimeException {
    private final UUID productId;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientStock(
            String message,
            UUID productId,
            BigDecimal available,
            BigDecimal requested
    ) {
        super(message);
        this.productId = productId;
        this.available = available;
        this.requested = requested;
    }

    public UUID getProductId() {
        return productId;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
