package com.site.toffCo.module.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.toffCo.infra.outbox.OutboxEvent;
import com.site.toffCo.infra.outbox.OutboxEventRepository;
import com.site.toffCo.module.admin.dto.AdminCarrinhoDTO;
import com.site.toffCo.module.admin.dto.AdminDashboardSummaryDTO;
import com.site.toffCo.module.admin.dto.AdminPedidoDTO;
import com.site.toffCo.module.admin.dto.AdminUserDTO;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceLineDTO;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // todas as queries são somente leitura
public class AdminDashboardService {

    private final CarrinhoRepository carrinhoRepository;
    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;

    private static final Map<PedidoStatus, List<PedidoStatus>> TRANSICOES_VALIDAS = Map.of(
            PedidoStatus.AGUARDANDO_PAGAMENTO, List.of(PedidoStatus.PAGO, PedidoStatus.CANCELADO),
            PedidoStatus.PAGO, List.of(PedidoStatus.EM_SEPARACAO, PedidoStatus.PRONTO, PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.EM_SEPARACAO, List.of(PedidoStatus.PRONTO, PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.PRONTO, List.of(PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.ENVIADO, List.of(PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.ENTREGUE, List.of(),
            PedidoStatus.CANCELADO, List.of()
    );

    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ─── RESUMO (cards do topo) ────────────────────────────────────

    public AdminDashboardSummaryDTO getSummary() {
        return new AdminDashboardSummaryDTO(
                carrinhoRepository.countCarrinhosComItens(),
                pedidoRepository.countByStatus(PedidoStatus.AGUARDANDO_PAGAMENTO),
                pedidoRepository.countByStatus(PedidoStatus.PAGO),
                pedidoRepository.countByStatus(PedidoStatus.CANCELADO),
                pedidoRepository.sumTotalByStatus(PedidoStatus.PAGO)
        );
    }

    // ─── CARRINHOS ATIVOS ─────────────────────────────────────────

    /**
     * Lista todos os carrinhos com pelo menos 1 item.
     * Útil para o admin enxergar quem está "quase comprando"
     * e tomar ações como enviar um cupom de recuperação.
     */
    public Page<AdminCarrinhoDTO> getCarrinhosAtivos(Pageable pageable) {
        return carrinhoRepository
                .findByCarrinhosComItem(pageable)
                .map(AdminCarrinhoDTO::from);
    }

    // ─── PEDIDOS ──────────────────────────────────────────────────

    /**
     * Lista todos os pedidos paginados, com filtro opcional por status.
     *
     * Paginação é importante aqui — em produção pode haver milhares
     * de pedidos. Sem paginação, a query traria tudo de uma vez.
     *
     * Uso: GET /api/admin/pedidos?page=0&size=20&status=PAGO
     */
    public Page<AdminPedidoDTO> getPedidos(PedidoStatus status, Pageable pageable) {
        Page<Pedido> page = status != null
                ? pedidoRepository.findByStatusOrderByDataCriacaoDesc(status, pageable)
                : pedidoRepository.findAllByOrderByDataCriacaoDesc(pageable);

        return page.map(AdminPedidoDTO::from);
    }

    /**
     * Detalhe completo de um pedido específico, incluindo todos os itens.
     */
    public AdminPedidoDTO getPedidoById(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + pedidoId));
        return AdminPedidoDTO.from(pedido);
    }

    /**
     * Todos os pedidos de um cliente específico.
     * Útil quando o admin clica num usuário e quer ver o histórico dele.
     */
    public List<AdminPedidoDTO> getPedidosByUser(UUID userId) {
        return pedidoRepository.findByUserIdOrderByDataCriacaoDesc(userId)
                .stream()
                .map(AdminPedidoDTO::from)
                .toList();
    }

    // ─── ATUALIZAÇÃO DE STATUS ────────────────────────────────────

    /**
     * Admin atualiza o status de um pedido manualmente.
     * Ex: marcar como EM_SEPARACAO, ENVIADO, ENTREGUE, CANCELADO.
     *
     * Quando o status muda para EM_SEPARACAO, gera automaticamente
     * o cupom fiscal (NF-e) no Odoo via Outbox.
     */
    @Transactional
    public AdminPedidoDTO updateStatusPedido(
            UUID pedidoId,
            PedidoStatus novoStatus
    ) {
        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Pedido não encontrado: " + pedidoId
                        )
                );

        PedidoStatus statusAtual = pedido.getStatus();
        List<PedidoStatus> permitidos = TRANSICOES_VALIDAS.getOrDefault(statusAtual, List.of());

        if (!permitidos.contains(novoStatus)) {
            throw new IllegalArgumentException(
                    "Transição inválida: " + statusAtual + " → " + novoStatus
                            + ". Transições permitidas: " + permitidos
            );
        }

        log.info(
                "Admin atualizando pedido {} de {} para {}",
                pedidoId,
                statusAtual,
                novoStatus
        );

        pedido.setStatus(novoStatus);

        // Quando gerente confirma o orçamento (EM_SEPARACAO), emite cupom fiscal
        if (novoStatus == PedidoStatus.EM_SEPARACAO) {
            gerarCupomFiscalOdoo(pedido);
        }

        // Força carregamento dos itens e user antes de montar o DTO
        pedido.getItens().size();
        if (pedido.getUser() != null) {
            pedido.getUser().getEmail();
        }

        return AdminPedidoDTO.from(pedido);
    }

    // ─── Geração de cupom fiscal via Outbox → Odoo ───────────────

    private void gerarCupomFiscalOdoo(Pedido pedido) {
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
            evento.setPayload(objectMapper().writeValueAsString(dto));
            outboxEventRepository.save(evento);

            log.info("Cupom fiscal Odoo agendado na outbox: pedido={}", pedido.getId());
        } catch (Exception e) {
            log.error("Erro ao agendar cupom fiscal Odoo: pedido={}", pedido.getId(), e);
        }
    }

    // ─── USUÁRIOS ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> findAllUsers(Pageable pageable) {
        return userRepository
                .findAll(pageable)
                .map(AdminUserDTO::from);
    }
}
