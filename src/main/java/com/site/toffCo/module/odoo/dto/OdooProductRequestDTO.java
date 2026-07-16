package com.site.toffCo.module.odoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OdooProductRequestDTO(
        @JsonProperty("name")
        String name,

        @JsonProperty("description_sale")
        String description,

        @JsonProperty("barcode")
        String barcode,

        @JsonProperty("list_price")
        BigDecimal price,

        @JsonProperty("qty_available")
        BigDecimal stock
) {
}