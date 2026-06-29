package com.site.toffCo.module.pagamentoitem.strategy;

import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("MONEY")
public class MoneyPaymantStrategy implements PagamentoStrategy{

    @Override
    public ResponseDTO processar(BigDecimal valor, UUID pedidoId) {
        return new ResponseDTO("MONEY", "PAGO", null, null, null);
    }
}
