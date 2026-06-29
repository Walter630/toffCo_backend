package com.site.toffCo.module.pagamentoitem.strategy;

import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("CARTAO_MAQUININHA")
public class MaquininhaPagamentoStrategy implements PagamentoStrategy {

    @Override
    public ResponseDTO processar(BigDecimal valor, UUID  pedidoId) {
        // 1. Chamar a API que se comunica com a maquininha (ex: Stone, Clover, etc.)
        // 2. Passar o valor e o identificador do terminal/maquina
        // 3. A API vai prender a requisição até o cliente passar o cartão ou dar timeout

        boolean sucesso = dispararQuandoAprovarMaquininha(valor, pedidoId);

        if (sucesso) {
            return new ResponseDTO("CARTAO_MAQUININHA", "PAGO", null, null, "Transação aprovada na maquina!");
        } else {
            return new ResponseDTO("CARTAO_MAQUININHA", "RECUSADO", null, null, "Transação Reprovada na maquina!");
        }
    }


    public boolean dispararQuandoAprovarMaquininha(BigDecimal valor, UUID pedidoId) {
        return true;
    }
}
