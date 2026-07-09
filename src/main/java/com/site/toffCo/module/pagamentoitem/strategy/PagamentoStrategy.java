package com.site.toffCo.module.pagamentoitem.strategy;

import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;

import java.math.BigDecimal;
import java.util.UUID;

public interface PagamentoStrategy {
    ResponseDTO processar(BigDecimal valor, UUID pedidoId);
    String getTipoPagamento();
}
