package com.site.toffCo.module.odoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OdooProductValuesDTO(

        String name,

        @JsonProperty("description_sale")
        String descriptionSale,

        String barcode,

        @JsonProperty("list_price")
        BigDecimal listPrice,

        @JsonProperty("type")
        String type
) {
}