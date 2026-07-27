package com.site.toffCo.module.produto.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductType {
    PLA("PLA"),
    PLA_PLUS("PLA+"),
    PLA_PRO("PLA Pro"),
    PLA_SILK("PLA Silk"),
    PLA_MATTE("PLA Matte"),
    PLA_WOOD("PLA Wood"),
    PLA_CF("PLA com Fibra de Carbono"),

    PETG("PETG"),
    PETG_PLUS("PETG+"),
    PETG_PRO("PETG Pro"),
    PETG_HF("PETG High Flow"),
    PETG_CF("PETG com Fibra de Carbono"),

    ABS("ABS"),
    ABS_PLUS("ABS+"),

    ASA("ASA"),
    TPU("TPU"),
    TPU_FLEX("TPU Flexível"),
    NYLON("Nylon"),
    NYLON_CF("Nylon com Fibra de Carbono"),
    PC("Policarbonato"),

    ACESSORIO("Acessório"),
    OUTRO("Outro");

    private final String descricao;
}
