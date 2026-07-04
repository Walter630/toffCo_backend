package com.site.toffCo.module.odoo.dto;

public record OdooStockWebhookPayload(String sku, int quantidadeSaida, String idVendaOdoo) {}
