package com.site.toffCo.module.odoo.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.toffCo.module.odoo.dto.OdooProductRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OdooClientRest {

    private static final Logger log =
            LoggerFactory.getLogger(OdooClientRest.class);

    private final String odooUrl = "https://sua-instancia.odoo.com";
    private final String db = "toffco_db";
    private final String login = "postgres";
    private final String password = "admin123";


    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    private final RestClient restClient;

    public OdooClientRest() {
        this.restClient = RestClient.builder()
                .baseUrl(odooUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /*
     * ============================================================
     * FASE 1 — AUTENTICAÇÃO
     * ============================================================
     *
     * O sistema pergunta ao Odoo:
     * "Quem sou eu?"
     *
     * O Odoo responde com o UID do usuário.
     */
    public Long authenticate() {
        Map<String, Object> payload = createJsonRpcPayload(
                "common",
                "login",
                List.of(db, login, password)
        );

        Map<String, Object> response = sendJsonRpc(payload);

        Object result = response.get("result");

        if (!(result instanceof Number number)) {
            throw new IllegalStateException(
                    "Falha ao autenticar no Odoo. Resposta: " + response
            );
        }

        Long uid = number.longValue();

        log.debug("Autenticação realizada no Odoo. uid={}", uid);

        return uid;
    }

    /*
     * ============================================================
     * FASE 2 — BUSCAR PRODUTO PELO CÓDIGO DE BARRAS
     * ============================================================
     */
    public Optional<Long> findProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }

        List<Map<String, Object>> products = searchRead(
                "product.product",
                List.of(
                        List.of("barcode", "=", barcode)
                ),
                List.of(
                        "id",
                        "name",
                        "barcode",
                        "default_code"
                ),
                1
        );

        if (products.isEmpty()) {
            log.info(
                    "Produto não encontrado no Odoo pelo barcode={}",
                    barcode
            );

            return Optional.empty();
        }

        Object id = products.getFirst().get("id");

        if (!(id instanceof Number number)) {
            throw new IllegalStateException(
                    "Produto encontrado no Odoo, mas o ID é inválido: " + id
            );
        }

        Long odooProductId = number.longValue();

        log.info(
                "Produto encontrado no Odoo: barcode={}, odooProductId={}",
                barcode,
                odooProductId
        );

        return Optional.of(odooProductId);
    }

    /*
     * ============================================================
     * FASE 3 — CRIAR PRODUTO
     * ============================================================
     */
    public Long createProduct(OdooProductRequestDTO request) {
        Map<String, Object> values = productValues(request);

        Object result = executeKw(
                "product.product",
                "create",
                List.of(values),
                Map.of()
        );

        if (!(result instanceof Number number)) {
            throw new IllegalStateException(
                    "Odoo não retornou o ID do produto criado. Resposta: "
                            + result
            );
        }

        Long odooProductId = number.longValue();

        log.info(
                "Produto criado no Odoo: odooProductId={}, barcode={}",
                odooProductId,
                request.barcode()
        );

        return odooProductId;
    }

    /*
     * ============================================================
     * FASE 4 — ATUALIZAR PRODUTO
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

        Map<String, Object> values = productValues(request);

        boolean updated = write(
                "product.product",
                List.of(odooProductId),
                values
        );

        if (!updated) {
            throw new IllegalStateException(
                    "Odoo não confirmou a atualização do produto "
                            + odooProductId
            );
        }

        log.info(
                "Produto atualizado no Odoo: odooProductId={}, barcode={}",
                odooProductId,
                request.barcode()
        );
    }

    /*
     * ============================================================
     * MOTOR — SEARCH_READ
     * ============================================================
     *
     * domain:
     * [
     *   ["barcode", "=", "789999999900"]
     * ]
     */
    private List<Map<String, Object>> searchRead(
            String model,
            List<List<Object>> domain,
            List<String> fields,
            int limit
    ) {
        Map<String, Object> kwargs = Map.of(
                "fields", fields,
                "limit", limit
        );

        Object result = executeKw(
                model,
                "search_read",
                List.of(domain),
                kwargs
        );

        return objectMapper.convertValue(
                result,
                new TypeReference<>() {
                }
        );
    }

    /*
     * ============================================================
     * MOTOR — WRITE
     * ============================================================
     *
     * O write recebe:
     *
     * [
     *   [ID_DO_PRODUTO],
     *   {
     *      "name": "...",
     *      "barcode": "..."
     *   }
     * ]
     */
    private boolean write(
            String model,
            List<Long> ids,
            Map<String, Object> values
    ) {
        Object result = executeKw(
                model,
                "write",
                List.of(ids, values),
                Map.of()
        );

        if (result instanceof Boolean booleanResult) {
            return booleanResult;
        }

        throw new IllegalStateException(
                "Resposta inválida ao atualizar no Odoo: " + result
        );
    }

    /*
     * ============================================================
     * MOTOR PRINCIPAL — EXECUTE_KW
     * ============================================================
     *
     * Este é o portal que chama qualquer método de qualquer model
     * do Odoo.
     */
    private Object executeKw(
            String model,
            String method,
            List<?> positionalArguments,
            Map<String, Object> keywordArguments
    ) {
        Long uid = authenticate();

        List<Object> args = List.of(
                db,
                uid,
                password,
                model,
                method,
                positionalArguments,
                keywordArguments
        );

        Map<String, Object> payload = createJsonRpcPayload(
                "object",
                "execute_kw",
                args
        );

        Map<String, Object> response = sendJsonRpc(payload);

        if (!response.containsKey("result")) {
            throw new IllegalStateException(
                    "Odoo respondeu sem o campo result: " + response
            );
        }

        return response.get("result");
    }

    /*
     * ============================================================
     * ENVIO HTTP
     * ============================================================
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendJsonRpc(
            Map<String, Object> payload
    ) {
        Map<String, Object> response = restClient.post()
                .uri("/jsonrpc")
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Odoo retornou uma resposta vazia"
            );
        }

        Object error = response.get("error");

        if (error != null) {
            log.error("Erro retornado pelo Odoo: {}", error);

            throw new IllegalStateException(
                    "Erro retornado pelo Odoo: " + error
            );
        }

        return response;
    }

    /*
     * ============================================================
     * CONSTRUÇÃO DO JSON-RPC
     * ============================================================
     */
    private Map<String, Object> createJsonRpcPayload(
            String service,
            String method,
            List<?> args
    ) {
        return Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "params", Map.of(
                        "service", service,
                        "method", method,
                        "args", args
                ),
                "id", System.currentTimeMillis()
        );
    }

    /*
     * ============================================================
     * MAPEAR DTO PARA CAMPOS DO ODOO
     * ============================================================
     */
    private Map<String, Object> productValues(
            OdooProductRequestDTO request
    ) {
        Map<String, Object> values = new HashMap<>();

        if (request.name() != null && !request.name().isBlank()) {
            values.put("name", request.name());
        }

        if (request.description() != null) {
            values.put("description_sale", request.description());
        }

        if (request.barcode() != null && !request.barcode().isBlank()) {
            values.put("barcode", request.barcode());
        }

        if (request.price() != null) {
            values.put("list_price", request.price());
        }

        /*
         * Não colocamos qty_available aqui.
         *
         * Estoque é outra missão: stock.quant,
         * ajuste de inventário ou movimentação.
         */

        return values;
    }

    /*
     * Seu método antigo pode continuar por enquanto.
     */
    public void executeInvoiceCreation(
            String customerCpf,
            List<Object> lines
    ) {
        Map<String, Object> invoiceData = Map.of(
                "move_type", "out_invoice",
                "ref", "Pedido via Site",
                "invoice_line_ids", lines
        );

        Object result = executeKw(
                "account.move",
                "create",
                List.of(invoiceData),
                Map.of()
        );

        log.info(
                "Nota fiscal criada no Odoo: customerCpf={}, result={}",
                customerCpf,
                result
        );
    }
}