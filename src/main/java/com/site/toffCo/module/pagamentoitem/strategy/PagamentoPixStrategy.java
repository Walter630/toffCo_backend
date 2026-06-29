package com.site.toffCo.module.pagamentoitem.strategy;

import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("PIX")
public class PagamentoPixStrategy implements PagamentoStrategy{
    @Override
    public ResponseDTO processar(BigDecimal valor, UUID pedidoId) {


        String qrCodeBase44 = "data:image/png;base64,..."; // valor da APi
        String copiaECola = "chave:9128jjwqndawunf82..."; // chave para copiar e colar do pix

        return new ResponseDTO("PÌX", "AGUARDANDO PAGAMENTO", qrCodeBase44, copiaECola, null);
    }
}
