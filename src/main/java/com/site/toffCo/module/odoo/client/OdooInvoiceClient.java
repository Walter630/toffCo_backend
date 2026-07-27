package com.site.toffCo.module.odoo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceLineDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP responsável por todas as chamadas ao Odoo
 * relacionadas à emissão de Nota Fiscal (account.move).
 *
 * O fluxo completo de emissão no Odoo é sempre em 3 etapas:
 *
 *   1. createInvoice()   → cria a fatura em status "draft"
 *   2. confirmInvoice()  → chama action_post, muda para "posted"
 *   3. sendNfe()         → chama action_send_nfe, envia para a SEFAZ
 *
 * Esse client executa as chamadas. A orquestração fica no OdooInvoiceService.
 *
 * A API REST do Odoo usada aqui é a mesma do OdooProductClient:
 *   POST /json/2/{model}/{method}
 */
@Slf4j
@Component
public class OdooInvoiceClient {

    private final RestClient restClient;

    public OdooInvoiceClient(
            RestClient.Builder builder,
            @Value("${odoo.base-url}") String baseUrl,
            @Value("${odoo.api-key}") String apiKey,
            @Value("${odoo.database}") String database
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "bearer " + apiKey.trim())
                .defaultHeader("X-Odoo-Database", database)
                .build();
    }

    // =========================================================================
    // ETAPA 1 — Criar fatura (draft)
    // =========================================================================

    /**
     * Cria uma fatura de saída (NF-e) no Odoo em status "draft".
     *
     * O Odoo usa "account.move" para representar faturas.
     * O campo "move_type" = "out_invoice" indica que é uma fatura de saída
     * (venda para cliente), que é o tipo correto para emitir uma NF-e.
     *
     * @param dto dados do pedido vindos da fila RabbitMQ
     * @return ID do account.move criado no Odoo
     */
    public Long createInvoice(OdooInvoiceCreateDTO dto) {
        log.info(
                "Criando fatura no Odoo: pedidoId={}, cliente={}, itens={}",
                dto.pedidoId(),
                dto.customerName(),
                dto.items().size()
        );

        /*
         * O campo "invoice_line_ids" usa o formato de comandos do ORM do Odoo.
         * Cada item é um array: [0, 0, { dados da linha }]
         *   0, 0  → comando "create" (criar nova linha)
         *   {...} → os campos da linha
         */
        List<Object> invoiceLines = dto.items().stream()
                .map(this::toOdooLineCommand)
                .map(line -> (Object) line)
                .toList();

        Map<String, Object> vals = Map.of(
                "move_type", "out_invoice",         // fatura de saída (NF-e)
                "partner_id", false,                // sem parceiro fixo — Odoo aceita sem vínculo
                "invoice_line_ids", invoiceLines,
                "narration", "Pedido: " + dto.pedidoId()
        );

        Map<String, Object> body = Map.of(
                "vals_list", List.of(vals)
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/account.move/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        Long invoiceId = extractId(response, "criação da fatura");

        log.info(
                "Fatura criada no Odoo: pedidoId={}, odooInvoiceId={}",
                dto.pedidoId(),
                invoiceId
        );

        return invoiceId;
    }

    // =========================================================================
    // ETAPA 2 — Confirmar fatura (draft → posted)
    // =========================================================================

    /**
     * Confirma a fatura no Odoo chamando o método "action_post".
     *
     * Após isso o status no Odoo muda de "draft" para "posted",
     * o número da nota é gerado e a fatura está pronta para ser
     * enviada para a SEFAZ.
     *
     * @param odooInvoiceId ID retornado pelo createInvoice()
     */
    public void confirmInvoice(Long odooInvoiceId) {
        log.info(
                "Confirmando fatura no Odoo (action_post): odooInvoiceId={}",
                odooInvoiceId
        );

        Map<String, Object> body = Map.of(
                "ids", List.of(odooInvoiceId)
        );

        restClient
                .post()
                .uri("/json/2/account.move/action_post")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        log.info(
                "Fatura confirmada no Odoo: odooInvoiceId={}",
                odooInvoiceId
        );
    }

    // =========================================================================
    // ETAPA 3 — Enviar para SEFAZ (posted → autorizada)
    // =========================================================================

    /**
     * Aciona o envio da NF-e para a SEFAZ via Odoo.
     *
     * Esse método chama "action_send_nfe" do módulo l10n_br_nfe.
     * O Odoo processa a transmissão de forma assíncrona e dispara um webhook
     * para /api/webhooks/odoo/invoice-status quando a SEFAZ responde.
     *
     * IMPORTANTE: para esse método funcionar, o Odoo precisa ter o módulo
     * l10n_br_nfe instalado e o certificado digital A1 configurado.
     *
     * @param odooInvoiceId ID da fatura confirmada
     */
    public void sendNfe(Long odooInvoiceId) {
        log.info(
                "Enviando NF-e para SEFAZ via Odoo: odooInvoiceId={}",
                odooInvoiceId
        );

        Map<String, Object> body = Map.of(
                "ids", List.of(odooInvoiceId)
        );

        restClient
                .post()
                .uri("/json/2/account.move/action_send_nfe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        log.info(
                "NF-e enviada para SEFAZ via Odoo: odooInvoiceId={}",
                odooInvoiceId
        );
    }

    // =========================================================================
    // CONSULTA — Buscar dados atuais de uma fatura
    // =========================================================================

    /**
     * Consulta os dados de uma fatura no Odoo pelo ID.
     *
     * Útil para buscar o número da nota e o nome gerado após o action_post.
     *
     * @param odooInvoiceId ID da fatura no Odoo
     * @return JsonNode com os campos solicitados, ou null se não encontrado
     */
    public JsonNode getInvoice(Long odooInvoiceId) {
        log.info(
                "Consultando fatura no Odoo: odooInvoiceId={}",
                odooInvoiceId
        );

        Map<String, Object> body = Map.of(
                "domain", List.of(
                        List.of("id", "=", odooInvoiceId)
                ),
                "fields", List.of(
                        "id",
                        "name",         // número da nota (INV/2025/00001)
                        "state",        // draft | posted | cancel
                        "move_type"
                ),
                "limit", 1
        );

        JsonNode response = restClient
                .post()
                .uri("/json/2/account.move/search_read")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.isArray() || response.isEmpty()) {
            log.warn(
                    "Fatura não encontrada no Odoo: odooInvoiceId={}",
                    odooInvoiceId
            );
            return null;
        }

        return response.get(0);
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /**
     * Converte um OdooInvoiceLineDTO no formato de comando de criação do Odoo.
     *
     * O Odoo usa um protocolo próprio para criar/editar registros relacionados
     * dentro do mesmo request. O formato é uma lista de 3 elementos:
     *
     *   [comando, id_existente, valores]
     *
     * Para criar um novo registro relacionado usamos:
     *   comando = 0  (CREATE)
     *   id      = 0  (ignorado no CREATE)
     *   valores = { campos do registro }
     */
    private List<Object> toOdooLineCommand(OdooInvoiceLineDTO line) {
        Map<String, Object> lineValues = Map.of(
                "name", line.name(),
                "quantity", line.quantity(),
                "price_unit", line.priceUnit()
        );

        /*
         * [0, 0, { dados }] → comando CREATE do ORM do Odoo
         */
        return List.of(0, 0, lineValues);
    }

    /**
     * Extrai o ID de uma resposta do Odoo, cobrindo os formatos possíveis:
     *   123
     *   [123]
     *   [{"id": 123}]
     *   {"id": 123}
     */
    private Long extractId(JsonNode response, String contexto) {
        if (response == null || response.isNull()) {
            throw new IllegalStateException(
                    "Odoo não retornou ID na operação: " + contexto
            );
        }

        if (response.canConvertToLong()) {
            return response.asLong();
        }

        if (response.isArray() && !response.isEmpty()) {
            JsonNode first = response.get(0);

            if (first.canConvertToLong()) {
                return first.asLong();
            }

            if (first.isObject()) {
                JsonNode idNode = first.get("id");
                if (idNode != null && idNode.canConvertToLong()) {
                    return idNode.asLong();
                }
            }
        }

        if (response.isObject()) {
            JsonNode idNode = response.get("id");
            if (idNode != null && idNode.canConvertToLong()) {
                return idNode.asLong();
            }
        }

        throw new IllegalStateException(
                "Formato inesperado na resposta do Odoo para: "
                        + contexto + " → " + response
        );
    }
}
