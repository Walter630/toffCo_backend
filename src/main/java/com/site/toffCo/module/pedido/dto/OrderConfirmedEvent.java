package com.site.toffCo.module.pedido.dto;

import java.util.List;

public record OrderConfirmedEvent(Long orderId, String custumerCpf, double total, List<OrderItemEvent> itens){}
