package com.site.toffCo.module.pagamentoitem.service;

import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.strategy.PagamentoStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PagamentoItemService {

    private final Map<String , PagamentoStrategy>  pagamentoStrategyMap;

    public PagamentoItemService(List<PagamentoStrategy> strategies) {
        this.pagamentoStrategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        strategy  -> strategy.getTipoPagamento().toUpperCase(),
                        strategy -> strategy
                ));
    }

    public ResponseDTO getPagamentoItem(PagamentoRequestDTO requestDTO) {
        // 1. Verifica se o DTO ou a forma de pagamento vieram nulos
        if (requestDTO == null || requestDTO.formaPagamento() == null) {
            throw new PaymentInvalidForm("A forma de pagamento não foi informada.");
        }

        String forma = requestDTO.formaPagamento().toUpperCase().trim();
        PagamentoStrategy pagamentoStrategy = this.pagamentoStrategyMap.get(forma);
        log.debug(requestDTO.formaPagamento());
        if (pagamentoStrategy == null) {
            throw new PaymentInvalidForm("Tipo de pagamento nao suportado: " + requestDTO.formaPagamento());
        }

        return pagamentoStrategy.processar(requestDTO.valor(), requestDTO.pedidoId());
    }
}
