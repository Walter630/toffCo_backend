package com.site.toffCo.module.pedido.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderConfirmedOdooEvent(UUID orderId, String customerCpf, BigDecimal total, List<OrderItemOdooEvent> items) {}
