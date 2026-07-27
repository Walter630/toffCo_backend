package com.site.toffCo.module.pagamentoitem.service;

import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.infra.exception.payment.PaymentNotFound;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoStatus;
import com.site.toffCo.module.pagamentoitem.repository.PagamentoItemRepository;
import com.site.toffCo.module.pagamentoitem.strategy.PagamentoStrategy;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PagamentoItemService {

    private final Map<String, PagamentoStrategy> pagamentoStrategyMap;
    private final PagamentoItemRepository pagamentoItemRepository;
    private final PedidoRepository pedidoRepository;

    public PagamentoItemService(
            List<PagamentoStrategy> strategies,
            PagamentoItemRepository pagamentoItemRepository,
            PedidoRepository pedidoRepository
    ) {
        this.pagamentoStrategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getTipoPagamento().toUpperCase(),
                        s -> s
                ));
        this.pagamentoItemRepository = pagamentoItemRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public ResponseDTO getPagamentoItem(PagamentoRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.formaPagamento() == null) {
            throw new PaymentInvalidForm("A forma de pagamento não foi informada.");
        }

        String forma = requestDTO.formaPagamento().toUpperCase().trim();
        PagamentoStrategy strategy = pagamentoStrategyMap.get(forma);

        if (strategy == null) {
            throw new PaymentInvalidForm("Tipo de pagamento não suportado: " + requestDTO.formaPagamento());
        }

        // Busca o pedido para vincular ao pagamento
        Pedido pedido = pedidoRepository.findById(requestDTO.pedidoId())
                .orElseThrow(() -> new PaymentNotFound("Pedido não encontrado: " + requestDTO.pedidoId()));

        // Processa via strategy (PIX, maquininha, dinheiro)
        ResponseDTO response = strategy.processar(requestDTO.valor(), requestDTO.pedidoId());
        log.debug("Pagamento processado: forma={}, status={}", forma, response.status());

        // Converte o status string da strategy para o enum
        PagamentoStatus status = mapStatus(response.status());

        // Persiste o registro de pagamento
        PagamentoItem pagamento = new PagamentoItem();
        pagamento.setPedido(pedido);
        pagamento.setValor(requestDTO.valor());
        pagamento.setFormaPagamento(forma);
        pagamento.setStatus(status);
        pagamento.setPixQrCodeBase64(response.qrCodeBase64());
        pagamento.setPixCopiaECola(response.copiaECola());
        pagamento.setMensagemRetorno(response.mensagem());
        pagamentoItemRepository.save(pagamento);

        // Se o pagamento foi aprovado, atualiza o status do pedido
        if (status == PagamentoStatus.APROVADO) {
            pedido.setStatus(PedidoStatus.PAGO);
            pedidoRepository.save(pedido);
            log.info("Pedido {} marcado como PAGO via {}", pedido.getId(), forma);
        }

        return response;
    }



    /**
     * Converte a string de status que vem das strategies para o enum PagamentoStatus.
     * As strategies existentes retornam: "AGUARDANDO PAGAMENTO", "PAGO", "RECUSADO"
     */
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
