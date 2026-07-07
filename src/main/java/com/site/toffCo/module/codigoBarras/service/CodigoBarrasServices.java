package com.site.toffCo.module.codigoBarras.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.site.toffCo.infra.exception.product.ProductNotFound;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodigoBarrasServices {
    private final ProdutoRepository produtoRepository;

    public String gerarCodigoEAN13(UUID produtoId) {
        long hash = Math.abs((long) produtoId.hashCode());
        String base = String.format("789%09d", hash % 1_000_000_000L); // 789 = prefixo Brasil (fictício aqui)
        int digitoVerificador = calcularDigitoVerificadorEAN13(base);
        return base + digitoVerificador;
    }

    private int calcularDigitoVerificadorEAN13(String base) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) {
            int digito = Character.getNumericValue(base.charAt(i));
            soma += (i % 2 == 0) ? digito : digito * 3;
        }
        int resto = soma % 10;
        return (resto == 0) ? 0 : 10 - resto;
    };

    public byte[] gerarImagemCodigoBarras(String codigo) throws WriterException, IOException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(codigo, BarcodeFormat.EAN_13, 300, 150);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] buscarImagemCodigoBarras(UUID produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProductNotFound("produto nao encontrado"));

        if (produto.getCodigoBarras() == null) {
            throw new IllegalArgumentException("Produto nao possui codigo de barras");
        }

        return produto.getImagemCodigoBarras();
    }
}
