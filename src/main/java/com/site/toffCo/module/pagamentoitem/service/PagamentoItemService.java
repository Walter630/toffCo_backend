package com.site.toffCo.module.pagamentoitem.service;

import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.strategy.PagamentoStrategy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PagamentoItemService {

    private final Map<String , PagamentoStrategy>  pagamentoStrategyMap;

    public PagamentoItemService(Map<String , PagamentoStrategy> pagamentoStrategyMap) {
        this.pagamentoStrategyMap = pagamentoStrategyMap;
    }

    public ResponseDTO getPagamentoItem(PagamentoRequestDTO requestDTO) {
        PagamentoStrategy pagamentoStrategy = this.pagamentoStrategyMap.get(requestDTO.formaPagamento());

        if (pagamentoStrategy == null) {
            throw new PaymentInvalidForm("Tipo de pagamento nao suportado: " + requestDTO.formaPagamento());
        }

        return pagamentoStrategy.processar(requestDTO.valor(), requestDTO.pedidoId());
    }
}
