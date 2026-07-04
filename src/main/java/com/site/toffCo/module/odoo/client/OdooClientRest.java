package com.site.toffCo.module.odoo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OdooClientRest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final String odooUrl = "https://sua-instancia.odoo.com";
    private final String db = "toffco_db";
    private final String login = "postgres";
    private final String password = "admin123";

    public OdooClientRest() {
        this.restClient = RestClient.builder()
                .baseUrl(odooUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void executeInvoiceCreation(String customerCpf, List<Object> lines) {
        // 1. Em cenários reais, você faria a autenticação para pegar o UID.
        // Para simplificar, vamos simular a chamada direta do execute_kw via JSON-RPC.

        var invoiceData = Map.of(
                "move_type", "out_invoice",
                "ref", "Pedido via Site",
                "invoice_line_ids", lines
        );

        var bodyPayload = Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "params", Map.of(
                        "service", "object",
                        "method", "execute_kw",
                        "args", List.of(db, 2, password, "account.move", "create", List.of(invoiceData))
                )
        );

        try {
            String jsonFormatado = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(bodyPayload);
            System.out.println("\n🚀 ====== [SIMULAÇÃO DE DISPARO PARA O ODOO] ======");
            System.out.println("Enviando nota fiscal para o CPF: " + customerCpf);
            System.out.println("PAYLOAD JSON GERADO PERFEITAMENTE:");
            System.out.println(jsonFormatado);
            System.out.println("===================================================\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    /*
        restClient.post()
                .uri("/jsonrpc")
                .body(bodyPayload)
                .retrieve()
                .toBodilessEntity();
                */
    }
}
