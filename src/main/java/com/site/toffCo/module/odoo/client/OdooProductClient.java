package com.site.toffCo.module.odoo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.site.toffCo.module.odoo.dto.OdooProductRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /*
     * ============================================================
     * PODER 1 — PROCURAR PELO CÓDIGO DE BARRAS
     * ============================================================
     */
    public Optional<Long> findProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> body = Map.of(
                "domain", List.of(
                        List.of("barcode", "=", barcode)
                ),
                "fields", List.of(
                        "id",
                        "name",
                        "barcode"
                ),
                "limit", 1
        );

        log.info(
                "Buscando produto no Odoo pelo barcode={}",
                barcode
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/product.template/search_read")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        log.debug(
                "Resposta da busca de produto no Odoo: {}",
                response
        );

        if (response == null ||
                response.isNull() ||
                !response.isArray() ||
                response.isEmpty()) {

            return Optional.empty();
        }

        JsonNode firstProduct = response.get(0);
        JsonNode idNode = firstProduct.get("id");

        if (idNode == null || !idNode.canConvertToLong()) {
            throw new IllegalStateException(
                    "Odoo encontrou o produto, mas retornou ID inválido: "
                            + response
            );
        }

        return Optional.of(idNode.asLong());
    }

    /*
     * ============================================================
     * PODER 2 — CRIAR
     * ============================================================
     */
    public Long createProduct(OdooProductRequestDTO request) {
        Map<String, Object> productValues =
                createProductValues(request);

        Map<String, Object> body = Map.of(
                "vals_list",
                List.of(productValues)
        );

        log.info(
                "Criando produto no Odoo: name={}, barcode={}",
                request.name(),
                request.barcode()
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/product.template/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        log.info(
                "Resposta da criação do produto no Odoo: {}",
                response
        );

        return extractCreatedId(response);
    }

    /*
     * ============================================================
     * PODER 3 — ATUALIZAR
     * ============================================================
     */
    public void updateProduct(
            Long odooProductId,
            OdooProductRequestDTO request
    ) {
        if (odooProductId == null) {
            throw new IllegalArgumentException(
                    "O ID do produto no Odoo não pode ser nulo"
            );
        }

        Map<String, Object> values =
                createProductValues(request);

        Map<String, Object> body = Map.of(
                "ids", List.of(odooProductId),
                "vals", values
        );

        log.info(
                "Atualizando produto no Odoo: odooProductId={}, barcode={}",
                odooProductId,
                request.barcode()
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/product.template/write")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        log.info(
                "Resposta da atualização no Odoo: {}",
                response
        );

        if (response == null
                || response.isNull()
                || !response.asBoolean(false)) {

            throw new IllegalStateException(
                    "Odoo não confirmou a atualização do produto "
                            + odooProductId
            );
        }
    }

    /*
     * ============================================================
     * MAPA DOS ATRIBUTOS
     * ============================================================
     */
    private Map<String, Object> createProductValues(
            OdooProductRequestDTO request
    ) {
        Map<String, Object> productValues = new HashMap<>();

        if (request.name() != null && !request.name().isBlank()) {
            productValues.put("name", request.name());
        }

        if (request.barcode() != null &&
                !request.barcode().isBlank()) {

            productValues.put(
                    "barcode",
                    request.barcode()
            );
        }

        if (request.price() != null) {
            productValues.put(
                    "list_price",
                    request.price()
            );
        }

        if (request.description() != null &&
                !request.description().isBlank()) {

            productValues.put(
                    "description_sale",
                    request.description()
            );
        }

        /*
         * Não colocamos qty_available.
         *
         * O estoque no Odoo é controlado por movimentação
         * e não pelo cadastro comum do produto.
         */

        return productValues;
    }

    /*
     * ============================================================
     * EXTRAIR ID DA RESPOSTA DO CREATE
     * ============================================================
     */
    private Long extractCreatedId(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new IllegalStateException(
                    "Odoo não retornou o ID do produto criado"
            );
        }

        /*
         * Caso o Odoo retorne:
         *
         * 123
         */
        if (response.canConvertToLong()) {
            return response.asLong();
        }

        /*
         * Caso retorne:
         *
         * [123]
         */
        if (response.isArray() && !response.isEmpty()) {
            JsonNode firstItem = response.get(0);

            if (firstItem.canConvertToLong()) {
                return firstItem.asLong();
            }

            /*
             * Caso retorne:
             *
             * [{"id": 123}]
             */
            if (firstItem.isObject()) {
                JsonNode idNode = firstItem.get("id");

                if (idNode != null &&
                        idNode.canConvertToLong()) {

                    return idNode.asLong();
                }
            }
        }

        /*
         * Caso retorne:
         *
         * {"id": 123}
         */
        if (response.isObject()) {
            JsonNode idNode = response.get("id");

            if (idNode != null &&
                    idNode.canConvertToLong()) {

                return idNode.asLong();
            }
        }

        throw new IllegalStateException(
                "Formato inesperado na criação do produto: "
                        + response
        );
    }
}