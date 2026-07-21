package com.site.toffCo.module.admin.dto;

import java.math.BigDecimal;

/**
 * Resumo geral exibido nos cards do topo do dashboard.
 *
 * carrinhosAtivos     → carrinhos com pelo menos 1 item (potenciais compras)
 * pedidosAguardando   → pagamento gerado mas não confirmado
 * pedidosPagos        → pagamentos confirmados
 * faturamentoTotal    → soma de todos os pedidos com status PAGO
 */
public record AdminDashboardSummaryDTO(
        long carrinhosAtivos,
        long pedidosAguardando,
        long pedidosPagos,
        long pedidosCancelados,
        BigDecimal faturamentoTotal
) {}
