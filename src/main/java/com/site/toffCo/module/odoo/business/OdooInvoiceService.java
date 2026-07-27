package com.site.toffCo.module.odoo.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.site.toffCo.module.odoo.client.OdooInvoiceClient;
import com.site.toffCo.module.odoo.dto.NotaFiscalStatus;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceStatusDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceWebhookDTO;
import com.site.toffCo.module.odoo.entity.NotaFiscal;
import com.site.toffCo.module.odoo.repository.NotaFiscalRepository;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestra o ciclo completo de emissão de NF-e via Odoo.
 *
 * Responsabilidades:
 *   1. emitir()          → recebe o evento da fila e executa as 3 etapas no Odoo
 *   2. processWebhook()  → recebe o retorno do Odoo (SEFAZ) e atualiza o banco
 *   3. consultarStatus() → retorna o status atual para o frontend
 *
 * Separação de responsabilidades:
 *   - OdooInvoiceClient   → faz as chamadas HTTP para o Odoo (sem lógica de negócio)
 *   - OdooInvoiceService  → orquestra o fluxo e persiste os estados (sem HTTP direto)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdooInvoiceService {

    private final OdooInvoiceClient odooInvoiceClient;
    private final NotaFiscalRepository notaFiscalRepository;
    private final PedidoRepository pedidoRepository;

    // =========================================================================
    // EMITIR — chamado pelo Consumer ao consumir a fila
    // =========================================================================

    /**
     * Executa o fluxo completo de emissão de NF-e no Odoo.
     *
     * Etapas:
     *   1. Cria um registro de NotaFiscal no banco com status PENDENTE
     *   2. Cria a fatura no Odoo (account.move) → status "draft"
     *   3. Confirma a fatura no Odoo (action_post) → status "posted"
     *   4. Envia para a SEFAZ (action_send_nfe) → Odoo processa em background
     *   5. Busca o número da nota gerado pelo Odoo e salva no banco
     *   6. Atualiza o status local para EMITIDA
     *
     * Se qualquer etapa falhar, o status é marcado como ERRO e a exception
     * sobe para o Consumer, que vai reprocessar via retry do RabbitMQ.
     *
     * @param dto dados vindos da fila odoo.invoice.create
     */
    @Transactional
    public void emitir(OdooInvoiceCreateDTO dto) {
        log.info(
                "Iniciando emissão de NF-e: pedidoId={}",
                dto.pedidoId()
        );

        /*
         * GUARDA DE IDEMPOTÊNCIA
         *
         * Se o Consumer processar a mesma mensagem duas vezes
         * (retry do RabbitMQ, por exemplo), não criamos uma nota duplicada.
         */
        if (notaFiscalRepository.existsByPedidoId(dto.pedidoId())) {
            log.warn(
                    "NF-e já processada para este pedido, ignorando: pedidoId={}",
                    dto.pedidoId()
            );
            return;
        }

        Pedido pedido = pedidoRepository
                .findById(dto.pedidoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pedido não encontrado para emissão de NF-e: "
                                + dto.pedidoId()
                ));

        /*
         * FASE 1 — Cria o registro no banco como PENDENTE.
         *
         * Fazemos isso antes de chamar o Odoo para que, se o sistema
         * reiniciar no meio do processo, possamos detectar e tratar
         * notas que ficaram presas em PENDENTE.
         */
        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setPedido(pedido);
        notaFiscal.setStatus(NotaFiscalStatus.PENDENTE);
        notaFiscalRepository.save(notaFiscal);

        try {
            /*
             * FASE 2 — Criar fatura no Odoo (status: draft)
             */
            Long odooInvoiceId = odooInvoiceClient.createInvoice(dto);
            notaFiscal.setOdooInvoiceId(odooInvoiceId);
            notaFiscalRepository.save(notaFiscal);

            /*
             * FASE 3 — Confirmar fatura no Odoo (draft → posted)
             *
             * Após esse passo, o Odoo gera o número da nota (ex: INV/2025/00001).
             */
            odooInvoiceClient.confirmInvoice(odooInvoiceId);

            /*
             * FASE 4 — Buscar o número da nota gerado pelo Odoo
             */
            String numeroNota = buscarNumeroNota(odooInvoiceId);
            notaFiscal.setNumeroNota(numeroNota);

            /*
             * FASE 5 — Enviar para a SEFAZ via Odoo (action_send_nfe)
             *
             * O Odoo processa o envio de forma assíncrona.
             * O retorno da SEFAZ chega via webhook (/api/webhooks/odoo/invoice-status).
             */
            odooInvoiceClient.sendNfe(odooInvoiceId);

            /*
             * FASE 6 — Marcar como EMITIDA no nosso banco.
             *
             * EMITIDA significa que enviamos para o Odoo e ele aceitou.
             * A mudança para AUTORIZADA acontece quando o webhook da SEFAZ chegar.
             */
            notaFiscal.setStatus(NotaFiscalStatus.EMITIDA);
            notaFiscalRepository.save(notaFiscal);

            log.info(
                    "NF-e emitida com sucesso: pedidoId={}, odooInvoiceId={}, numeroNota={}",
                    dto.pedidoId(),
                    odooInvoiceId,
                    numeroNota
            );

        } catch (Exception e) {
            /*
             * Se qualquer etapa falhar, registramos o erro no banco
             * e relançamos a exception para o Consumer — que vai recolocar
             * na fila para retry (configurado no application.yaml).
             */
            log.error(
                    "Erro na emissão de NF-e: pedidoId={}, erro={}",
                    dto.pedidoId(),
                    e.getMessage(),
                    e
            );

            notaFiscal.setStatus(NotaFiscalStatus.ERRO);
            notaFiscal.setMensagemErro(truncar(e.getMessage(), 1000));
            notaFiscalRepository.save(notaFiscal);

            /*
             * Relançamos para que o RabbitMQ faça o retry.
             * Após esgotar os retries, vai para a DLQ.
             */
            throw new RuntimeException(
                    "Falha na emissão de NF-e para pedidoId=" + dto.pedidoId(),
                    e
            );
        }
    }

    // =========================================================================
    // PROCESSAR WEBHOOK — chamado pelo OdooWebhookController
    // =========================================================================

    /**
     * Processa o retorno do Odoo após a SEFAZ responder.
     *
     * O Odoo chama nosso endpoint /api/webhooks/odoo/invoice-status
     * com o resultado da transmissão.
     *
     * Estados possíveis no campo "nfe_state" do Odoo (módulo l10n_br_nfe):
     *   autorizada       → SEFAZ autorizou, chave de acesso preenchida
     *   denegada         → SEFAZ negou (CNPJ irregular, etc.)
     *   erro_autorizacao → erro técnico no envio
     *   cancelada        → nota cancelada
     *
     * @param payload dados enviados pelo Odoo no webhook
     */
    @Transactional
    public void processarWebhook(OdooInvoiceWebhookDTO payload) {
        log.info(
                "Webhook NF-e recebido: odooInvoiceId={}, state={}, nfeState={}",
                payload.getInvoiceId(),
                payload.getState(),
                payload.getNfeState()
        );

        NotaFiscal notaFiscal = notaFiscalRepository
                .findByOdooInvoiceId(payload.getInvoiceId())
                .orElseGet(() -> {
                    /*
                     * Pode acontecer se o webhook chegar antes do banco ser atualizado
                     * (condição de corrida). Logamos e ignoramos — o Odoo vai reenviar.
                     */
                    log.warn(
                            "Webhook recebido para odooInvoiceId={} sem NotaFiscal no banco. Ignorando.",
                            payload.getInvoiceId()
                    );
                    return null;
                });

        if (notaFiscal == null) {
            return;
        }

        /*
         * Atualiza os dados retornados pelo Odoo / SEFAZ.
         */
        if (payload.getInvoiceName() != null) {
            notaFiscal.setNumeroNota(payload.getInvoiceName());
        }

        if (payload.getAccessKey() != null) {
            notaFiscal.setChaveAcesso(payload.getAccessKey());
        }

        if (payload.getPdfUrl() != null) {
            notaFiscal.setUrlDanfe(payload.getPdfUrl());
        }

        if (payload.getXmlUrl() != null) {
            notaFiscal.setUrlXml(payload.getXmlUrl());
        }

        /*
         * Traduz o estado do Odoo para o nosso enum interno.
         */
        NotaFiscalStatus novoStatus = traduzirStatus(
                payload.getState(),
                payload.getNfeState()
        );

        notaFiscal.setStatus(novoStatus);
        notaFiscalRepository.save(notaFiscal);

        log.info(
                "NotaFiscal atualizada via webhook: odooInvoiceId={}, novoStatus={}",
                payload.getInvoiceId(),
                novoStatus
        );
    }

    // =========================================================================
    // CONSULTAR STATUS — chamado pelo controller de consulta
    // =========================================================================

    /**
     * Retorna o status atual de uma NF-e pelo ID do pedido.
     *
     * @param pedidoId UUID do pedido
     * @return DTO com os dados da nota para exibição no frontend
     */
    @Transactional(readOnly = true)
    public OdooInvoiceStatusDTO consultarStatus(UUID pedidoId) {
        NotaFiscal notaFiscal = notaFiscalRepository
                .findByPedidoId(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nenhuma NF-e encontrada para o pedido: " + pedidoId
                ));

        return new OdooInvoiceStatusDTO(
                pedidoId,
                notaFiscal.getStatus(),
                notaFiscal.getNumeroNota(),
                notaFiscal.getChaveAcesso(),
                notaFiscal.getUrlDanfe(),
                notaFiscal.getUrlXml(),
                notaFiscal.getMensagemErro()
        );
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /**
     * Busca o número da nota gerado pelo Odoo após o action_post.
     * Retorna null se não conseguir — não é um erro fatal nesse ponto.
     */
    private String buscarNumeroNota(Long odooInvoiceId) {
        try {
            JsonNode invoice = odooInvoiceClient.getInvoice(odooInvoiceId);

            if (invoice != null && invoice.has("name")) {
                String name = invoice.get("name").asText();

                /*
                 * O Odoo retorna "/" quando a fatura ainda não tem número.
                 * Ignoramos esse valor.
                 */
                if (!"/".equals(name) && !name.isBlank()) {
                    return name;
                }
            }
        } catch (Exception e) {
            log.warn(
                    "Não foi possível buscar o número da nota: odooInvoiceId={}, erro={}",
                    odooInvoiceId,
                    e.getMessage()
            );
        }

        return null;
    }

    /**
     * Traduz os campos "state" e "nfe_state" do Odoo para o nosso enum.
     *
     * Prioridade: nfe_state tem precedência sobre state,
     * pois é mais específico para o contexto de NF-e.
     */
    private NotaFiscalStatus traduzirStatus(String state, String nfeState) {
        if (nfeState != null) {
            return switch (nfeState.toLowerCase()) {
                case "autorizada"        -> NotaFiscalStatus.AUTORIZADA;
                case "cancelada"         -> NotaFiscalStatus.CANCELADA;
                case "denegada",
                     "erro_autorizacao"  -> NotaFiscalStatus.ERRO;
                default                  -> NotaFiscalStatus.EMITIDA;
            };
        }

        if (state != null) {
            return switch (state.toLowerCase()) {
                case "posted" -> NotaFiscalStatus.EMITIDA;
                case "cancel" -> NotaFiscalStatus.CANCELADA;
                default       -> NotaFiscalStatus.PENDENTE;
            };
        }

        return NotaFiscalStatus.PENDENTE;
    }

    /**
     * Garante que a mensagem de erro não ultrapasse o limite da coluna no banco.
     */
    private String truncar(String mensagem, int limite) {
        if (mensagem == null) {
            return "Erro desconhecido";
        }
        return mensagem.length() > limite
                ? mensagem.substring(0, limite)
                : mensagem;
    }
}
