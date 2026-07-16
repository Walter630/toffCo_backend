package com.site.toffCo.module.odoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
//@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OdooStockWebhookDTO {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("product_barcode")
    private String productBarcode;
    @JsonProperty("quantity")
    private BigDecimal quantity;
    @JsonProperty("location_dest_usage")
    private String locationDestUsage;
    private String reference;
}
