package com.site.toffCo.module.odoo.business;

import com.site.toffCo.module.odoo.client.OdooClientRest;
import com.site.toffCo.module.pedido.service.OrderConfirmedOdooEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OdooMappingService {

    // Em vez de Rabbit, ele precisa do Client HTTP do Odoo para enviar os dados
    private final OdooClientRest odooRestClient;

    /**
     * Pega o evento do seu sistema e traduz para o formato de faturamento do Odoo
     */
    public void traduzirEEnviarFatura(OrderConfirmedOdooEvent event) {

        List<Object> odooLines = new ArrayList<>();

        for (var item : event.items()) {
            // O Odoo tem uma sintaxe muito bizarra para criar linhas via API.
            // Ele espera uma lista contendo: [0, 0, { mapa_com_os_valores }]
            // O número 0 significa "Comando para criar um novo registro filho"
            Map<String, Object> valoresDaLinha = Map.of(
                    "product_id", item.sku(), // ID ou SKU do produto que o Odoo reconheça
                    "quantity", item.quantity(),
                    "price_unit", item.price()
            );

            // Monta o padrão [0, 0, dados] exigido pelo ORM do Odoo
            odooLines.add(List.of(0, 0, valoresDaLinha));
        }

        // Agora que os dados estão traduzidos, passamos para o Client fazer o POST real
        odooRestClient.executeInvoiceCreation(event.customerCpf(), odooLines);
    }
}