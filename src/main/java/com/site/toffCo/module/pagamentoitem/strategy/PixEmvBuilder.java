package com.site.toffCo.module.pagamentoitem.strategy;

import java.math.BigDecimal;
import java.util.UUID;

public class PixEmvBuilder {
    private static final String CHAVE_PIX = "65648940000137";
    private static final String NOME_RECEBEDOR = "TOFF BRASIL";
    private static final String CIDADE = "UBERLANDIA";

    public static String gerarPayload(BigDecimal valor, UUID pedidoId) {
        String valorFormatado = valor.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
        String txid = pedidoId.toString().replaceAll("-", "").substring(0, 25);

        String merchantAccountInfo =
                campo("00", "BR.GOV.BCB.PIX") +
                        campo("01", CHAVE_PIX);

        String additionalData = campo("05", txid);

        StringBuilder payload = new StringBuilder();
        payload.append(campo("00", "01"));      //paylaod formatado
        payload.append(campo("01", "12"));      //uso Unico
        payload.append(campo("26", merchantAccountInfo));   //Conta pix
        payload.append(campo("52", "0000"));        // MCC
        payload.append(campo("53", "986"));         // Moeda BRL
        payload.append(campo("54", valorFormatado));      // Valor
        payload.append(campo("58", "BR"));          // Pais
        payload.append(campo("59", NOME_RECEBEDOR));      // Nome
        payload.append(campo("60", CIDADE));                // Cidade
        payload.append(campoComposto("62", additionalData));  //TXID

        payload.append("6304");  // tag+tamanho do CRC, valor calculado depois
        String crc = calcularCRC16(payload.toString());
        return payload.append(crc).toString();

    }

    private static String campo(String tag, String valor) {
        String tamanho = String.format("%02d", valor.length());
        return tag + tamanho + valor;
    }

    private static String campoComposto(String tag, String valorMontado) {
        String tamanho = String.format("%02d", valorMontado.length());
        return tag + tamanho + valorMontado;
    }

    private static String calcularCRC16(String payload) {
        int polinomio = 0x1021;
        int resultado = 0xFFFF;

        byte[] bytes = payload.getBytes();
        for (byte b : bytes) {
            resultado ^= (b << 8);
            for (int i = 0; i < 8; i++) {
                if ((resultado & 0x8000) != 0) {
                    resultado = (resultado << 1) ^ polinomio;
                } else {
                    resultado <<= 1;
                }
            }
        }
        resultado &= 0xFFFF;
        return String.format("%04X", resultado);
    }
}
