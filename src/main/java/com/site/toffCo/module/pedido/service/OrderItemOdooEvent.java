package com.site.toffCo.module.pedido.service;

import java.math.BigDecimal;

public record OrderItemOdooEvent(String sku, int quantity, BigDecimal price) {}
