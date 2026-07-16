package com.site.toffCo.module.odoo.client;

import com.site.toffCo.module.odoo.dto.OdooProductRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OdooProductClient {


    private final RestClient restClient;

    public OdooProductClient(
            RestClient.Builder builder,
            @Value("${odoo.base-url}") String baseUrl,
            @Value("${odoo.api-key}") String apiKey,
            @Value("${odoo.database}") String database
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Authorization",
                        "bearer " + apiKey.trim()
                )
                .defaultHeader(
                        "X-Odoo-Database",
                        database
                )
                .build();
    }

    public JsonNode createProduct(OdooProductRequestDTO request) {

        Map<String, Object> productValues = new HashMap<>();

        productValues.put("name", request.name());
        productValues.put("barcode", request.barcode());
        productValues.put("list_price", request.price());

        if (request.description() != null) {
            productValues.put(
                    "description_sale",
                    request.description()
            );
        }

        /*
         * O nome precisa ser exatamente "vals_list".
         * Não pode ser valsList, vals ou values.
         */
        Map<String, Object> body = new HashMap<>();
        body.put("vals_list", List.of(productValues));

        log.info(
                "Criando produto no Odoo: name={}, barcode={}",
                request.name(),
                request.barcode()
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/product.template/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body) // precisa ser body, não request
                .retrieve()
                .body(JsonNode.class);

        log.info("Resposta real do Odoo: {}", response);

        if (response == null || response.isNull()) {
            throw new IllegalStateException(
                    "Odoo não retornou o ID do produto criado"
            );
        }

        return response;
    }
}