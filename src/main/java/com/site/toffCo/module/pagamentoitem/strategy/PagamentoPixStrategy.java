package com.site.toffCo.module.pagamentoitem.strategy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

@Component("PIX")
public class PagamentoPixStrategy implements PagamentoStrategy{
    @Override
    public ResponseDTO processar(BigDecimal valor, UUID pedidoId) {

        String copiaECola  = PixEmvBuilder.gerarPayload(valor, pedidoId); // valor da APi
        String qrCodeBase44 = gerarQrCodeBase64(copiaECola);  // chave para copiar e colar do pix

        return new ResponseDTO("PÌX", "AGUARDANDO PAGAMENTO", qrCodeBase44, copiaECola, null);
    }

    @Override
    public String getTipoPagamento() {
        return "PIX";
    }

    public String gerarQrCodeBase64(String conteudo) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix matrix = qrCodeWriter.encode(
                    conteudo, BarcodeFormat.QR_CODE, 300, 300
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "png", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar imagem de Base64.", e);
        }
    }
}
