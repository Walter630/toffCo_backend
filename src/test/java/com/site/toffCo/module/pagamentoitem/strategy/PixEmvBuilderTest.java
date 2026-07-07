package com.site.toffCo.module.pagamentoitem.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PixEmvBuilderTest {

    @Test
    void gerarPayload() {
        String payload = PixEmvBuilder.gerarPayload(
                new BigDecimal("120.00"),
                UUID.randomUUID()
        );
        System.out.printf("Payload: %s\n", payload);
    }
}