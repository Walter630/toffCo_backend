package com.site.toffCo.module.admin.service;

import com.site.toffCo.module.admin.dto.AdminCarrinhoDTO;
import com.site.toffCo.module.admin.dto.AdminDashboardSummaryDTO;
import com.site.toffCo.module.admin.dto.AdminPedidoDTO;
import com.site.toffCo.module.admin.dto.AdminUserDTO;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // todas as queries são somente leitura
public class AdminDashboardService {

    private final CarrinhoRepository carrinhoRepository;
    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;

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
    public List<AdminCarrinhoDTO> getCarrinhosAtivos() {
        return carrinhoRepository.findCarrinhosComItens()
                .stream()
                .map(AdminCarrinhoDTO::from)
                .toList();
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

        List<AdminPedidoDTO> content = page.getContent()
                .stream()
                .map(AdminPedidoDTO::from)
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
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
     */
    @Transactional
    public AdminPedidoDTO updateStatusPedido(UUID pedidoId, PedidoStatus novoStatus) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + pedidoId));

        log.info("Admin atualizando pedido {} de {} para {}", pedidoId, pedido.getStatus(), novoStatus);
        pedido.setStatus(novoStatus);
        pedidoRepository.save(pedido);
        return AdminPedidoDTO.from(pedido);
    }


    @Transactional(readOnly = true)
    public List<AdminUserDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(AdminUserDTO::from)
                .toList();
    }

}
