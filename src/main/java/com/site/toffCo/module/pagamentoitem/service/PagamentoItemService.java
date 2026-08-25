package com.site.toffCo.module.pagamentoitem.service;

import tools.jackson.databind.ObjectMapper;
import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.infra.exception.payment.PaymentNotFound;
import com.site.toffCo.infra.outbox.OutboxEvent;
import com.site.toffCo.infra.outbox.OutboxEventRepository;
import com.site.toffCo.infra.rabbitMQ.EmailService;
import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceLineDTO;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoStatus;
import com.site.toffCo.module.pagamentoitem.repository.PagamentoItemRepository;
import com.site.toffCo.module.pagamentoitem.strategy.PagamentoStrategy;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.service.WhatzapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PagamentoItemService {

    private static final Map<String, String> ALIASES_PAGAMENTO = Map.of(
            "DINHEIRO", "MONEY",
            "CARTAO", "CARTAO_MAQUININHA",
            "CREDITO", "CARTAO_MAQUININHA",
            "DEBITO", "CARTAO_MAQUININHA"
    );

    private final Map<String, PagamentoStrategy> pagamentoStrategyMap;
    private final PagamentoItemRepository pagamentoItemRepository;
    private final PedidoRepository pedidoRepository;
    private final AuthUtil authUtil;
    private final OutboxEventRepository outboxEventRepository;
    private final EmailService emailService;
    private final WhatzapService whatzapService;
    private final org.thymeleaf.TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

  //  private ObjectMapper objectMapper() {
      //  return new ObjectMapper();
    //}

    private String normalizarFormaPagamento(String forma) {
        String normalizado = forma.toUpperCase().trim();
        return ALIASES_PAGAMENTO.getOrDefault(normalizado, normalizado);
    }

    @Value("${toffco.gerente.email:}")
    private String gerenteEmail;

    @Value("${toffco.gerente.whatsapp:}")
    private String gerenteWhatsapp;

    public PagamentoItemService(
            List<PagamentoStrategy> strategies,
            PagamentoItemRepository pagamentoItemRepository,
            PedidoRepository pedidoRepository,
            AuthUtil authUtil,
            OutboxEventRepository outboxEventRepository,
            EmailService emailService,
            WhatzapService whatzapService,
            org.thymeleaf.TemplateEngine templateEngine,
            ObjectMapper objectMapper
    ) {
        this.pagamentoStrategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getTipoPagamento().toUpperCase(),
                        s -> s
                ));
        this.pagamentoItemRepository = pagamentoItemRepository;
        this.pedidoRepository = pedidoRepository;
        this.authUtil = authUtil;
        this.outboxEventRepository = outboxEventRepository;
        this.emailService = emailService;
        this.whatzapService = whatzapService;
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResponseDTO getPagamentoItem(PagamentoRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.formaPagamento() == null) {
            throw new PaymentInvalidForm("A forma de pagamento não foi informada.");
        }

        String forma = normalizarFormaPagamento(requestDTO.formaPagamento());
        PagamentoStrategy strategy = pagamentoStrategyMap.get(forma);

        if (strategy == null) {
            throw new PaymentInvalidForm("Tipo de pagamento não suportado: " + requestDTO.formaPagamento());
        }

        User user = authUtil.getUserLogado();

        Pedido pedido = pedidoRepository
                .findByIdAndUserIdForUpdate(requestDTO.pedidoId(), user.getId())
                .orElseThrow(() ->
                        new PaymentNotFound("Pedido não encontrado")
                );

        if (pedido.getStatus() != PedidoStatus.AGUARDANDO_PAGAMENTO) {
            throw new PaymentInvalidForm("Pedido não está aguardando pagamento");
        }

        if (pagamentoItemRepository.findByPedidoIdAndStatus(pedido.getId(), PagamentoStatus.APROVADO).isPresent()) {
            throw new PaymentInvalidForm("Pedido já possui pagamento aprovado");
        }

        // Bloqueia criação de novo pagamento se já existe um AGUARDANDO (ex: QR PIX já gerado)
        if (pagamentoItemRepository.findByPedidoIdAndStatus(pedido.getId(), PagamentoStatus.AGUARDANDO).isPresent()) {
            throw new PaymentInvalidForm("Já existe um pagamento pendente para este pedido");
        }

        BigDecimal valorPedido = pedido.getTotal();
        if (requestDTO.valor() == null || requestDTO.valor().compareTo(valorPedido) != 0) {
            throw new PaymentInvalidForm("O valor do pagamento não corresponde ao valor do pedido");
        }

        // Processa via strategy (PIX, maquininha, dinheiro)
        ResponseDTO response = strategy.processar(valorPedido, pedido.getId());
        log.debug("Pagamento processado: forma={}, status={}", forma, response.status());

        PagamentoStatus status = mapStatus(response.status());

        // Persiste o registro de pagamento
        PagamentoItem pagamento = new PagamentoItem();
        pagamento.setPedido(pedido);
        pagamento.setValor(valorPedido);
        pagamento.setFormaPagamento(forma);
        pagamento.setStatus(status);
        pagamento.setPixQrCodeBase64(response.qrCodeBase64());
        pagamento.setPixCopiaECola(response.copiaECola());
        pagamento.setMensagemRetorno(response.mensagem());
        pagamentoItemRepository.save(pagamento);

        // Se o pagamento foi aprovado, atualiza pedido e agenda evento Odoo
        if (status == PagamentoStatus.APROVADO) {
            pedido.setStatus(PedidoStatus.PAGO);
            pedidoRepository.save(pedido);
            log.info("Pedido {} marcado como PAGO via {}", pedido.getId(), forma);

            salvarEventoOdoo(pedido);
            notificarGerente(pedido, forma);
        }

        return response;
    }

    @Transactional
    public ResponseDTO updatePagamentoItem(PagamentoRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.formaPagamento() == null) {
            throw new PaymentInvalidForm("Dados de pagamento inválidos.");
        }
        String forma = normalizarFormaPagamento(requestDTO.formaPagamento());
        PagamentoStrategy strategy = pagamentoStrategyMap.get(forma);
        if (strategy == null) {
            throw new PaymentInvalidForm("Tipo de pagamento não suportado: " + forma);
        }
        return strategy.processar(requestDTO.valor(), requestDTO.pedidoId());
    }

    // ─── Outbox: salva evento Odoo na mesma transação do pagamento ───

    private void salvarEventoOdoo(Pedido pedido) {
        try {
            List<OdooInvoiceLineDTO> linhas = pedido.getItens().stream()
                    .map(item -> new OdooInvoiceLineDTO(
                            item.getNomeProduto(),
                            item.getQuantidade(),
                            item.getPrecoUnitario()
                    ))
                    .toList();

            var dto = new OdooInvoiceCreateDTO(
                    pedido.getId(),
                    pedido.getUser().getUsername(),
                    pedido.getUser().getCpf(),
                    pedido.getUser().getEmail(),
                    linhas
            );

            OutboxEvent evento = new OutboxEvent();
            evento.setAggregateId(pedido.getId());
            evento.setTypeEvent("ODOO_INVOICE");
            evento.setPayload(objectMapper.writeValueAsString(dto));
            outboxEventRepository.save(evento);

            log.debug("Evento ODOO_INVOICE salvo na outbox: pedido={}", pedido.getId());
        } catch (Exception e) {
            log.error("Erro ao salvar evento Odoo na outbox: pedido={}", pedido.getId(), e);
            throw new RuntimeException("Falha ao salvar evento Odoo na outbox: " + pedido.getId(), e);
        }
    }

    // ─── Notificação: avisa o gerente que um pedido foi pago ───

    private void notificarGerente(Pedido pedido, String formaPagamento) {
        // Notificação por e-mail
        if (gerenteEmail != null && !gerenteEmail.isBlank()) {
            try {
                String assunto = "Novo pedido pago - #" + pedido.getId().toString().substring(0, 8).toUpperCase();

                org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                context.setVariable("pedidoId", pedido.getId().toString().substring(0, 8).toUpperCase());
                context.setVariable("clienteNome", pedido.getUser().getUsername());
                context.setVariable("clienteEmail", pedido.getUser().getEmail());
                context.setVariable("clienteTelefone", pedido.getUser().getPhone());
                context.setVariable("formaPagamento", formaPagamento);
                context.setVariable("total", String.format("R$ %.2f", pedido.getTotal()));

                List<Map<String, Object>> itensData = pedido.getItens().stream()
                        .map(item -> {
                            Map<String, Object> m = new java.util.LinkedHashMap<>();
                            m.put("nome", item.getNomeProduto());
                            m.put("quantidade", item.getQuantidade());
                            m.put("subtotal", String.format("R$ %.2f", item.getSubtotal()));
                            return m;
                        })
                        .toList();
                context.setVariable("itens", itensData);

                String corpo = templateEngine.process("notificacao-gerente", context);

                emailService.sendTemplateEmail(gerenteEmail, assunto, corpo);
                log.info("E-mail de notificação enviado ao gerente: pedido={}", pedido.getId());
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail ao gerente: pedido={}", pedido.getId(), e);
            }
        }

        // Notificação por WhatsApp
        if (gerenteWhatsapp != null && !gerenteWhatsapp.isBlank()) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("*Novo pedido pago!*\n\n");
                sb.append("*Pedido:* #").append(pedido.getId().toString().substring(0, 8).toUpperCase()).append("\n");
                sb.append("*Cliente:* ").append(pedido.getUser().getUsername()).append("\n");
                sb.append("*Total:* R$ ").append(String.format("%.2f", pedido.getTotal())).append("\n");
                sb.append("*Forma:* ").append(formaPagamento).append("\n\n");
                sb.append("*Itens:*\n");
                for (var item : pedido.getItens()) {
                    sb.append("  - ").append(item.getNomeProduto())
                      .append(" x").append(item.getQuantidade())
                      .append(" (R$ ").append(String.format("%.2f", item.getSubtotal())).append(")\n");
                }
                sb.append("\nSepare o pedido!");

                whatzapService.sendMessage(new SendMessageRequest(
                        gerenteWhatsapp,
                        sb.toString(),
                        1000
                ));
                log.info("WhatsApp de notificação enviado ao gerente: pedido={}", pedido.getId());
            } catch (Exception e) {
                log.warn("Falha ao enviar WhatsApp ao gerente: pedido={}", pedido.getId(), e);
            }
        }
    }


    private PagamentoStatus mapStatus(String statusStr) {
        if (statusStr == null) return PagamentoStatus.AGUARDANDO;
        return switch (statusStr.toUpperCase().trim()) {
            case "PAGO"                 -> PagamentoStatus.APROVADO;
            case "RECUSADO"             -> PagamentoStatus.RECUSADO;
            case "AGUARDANDO PAGAMENTO" -> PagamentoStatus.AGUARDANDO;
            default                     -> PagamentoStatus.AGUARDANDO;
        };
    }
}
