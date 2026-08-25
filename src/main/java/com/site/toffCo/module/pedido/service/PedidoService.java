package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarIsEmpty;
import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.exception.item.QuantidadInvalid;
import com.site.toffCo.infra.exception.payment.PaymentNotFound;
import com.site.toffCo.infra.exception.stock.InsufficientStock;
import com.site.toffCo.module.carrinho.dto.CarrinhoStatus;
import com.site.toffCo.module.pedido.dto.BaixaEstoqueRequestDTO;
import com.site.toffCo.module.pedido.dto.PedidoCheckoutResponseDTO;
import com.site.toffCo.module.pedido.dto.PedidoEvent;
import com.site.toffCo.module.pedido.dto.VendaPresencialRequestDTO;
import com.site.toffCo.infra.rabbitMQ.PedidoProducer;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.pedido.dto.PedidoResumoDTO;
import com.site.toffCo.module.pedido.entity.ItemPedido;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.domain.exception.ProductNotFound;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoProducer pedidoProducer;
    private final CarrinhoRepository carrinhoRepository;
    private final ProdutoRepository produtoRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = "carrinhos", key = "#userId")
    public PedidoCheckoutResponseDTO realizarCheckout(UUID userId, String idempotency) {
        Optional<Pedido> pedidoExistente = pedidoRepository.findByUserIdAndIdempotencyKey(userId, idempotency);

        if (pedidoExistente.isPresent()) {
            return new PedidoCheckoutResponseDTO(
                    pedidoExistente.get().getId(),
                    pedidoExistente.get().getTotal()
            );
        }

        // 1. Busca o carrinho do usuário com itens (fetch join)
        Carrinho carrinho = carrinhoRepository
                .findByCarrinhoCompletoForUpdate(userId)
                .orElseThrow(() -> new CarNotFound("Car not found"));

        if (carrinho.getCarrinhoStatus() == CarrinhoStatus.CONVERTIDO) {
            throw new CarIsEmpty("Este carrinho já foi convertido em pedido");
        }

        if (carrinho.getItens().isEmpty()) {
            throw new CarIsEmpty("Carrinho vazio!");
        }

        // 2. Cria o pedido base
        Pedido pedido = new Pedido();
        pedido.setUser(carrinho.getUser());
        pedido.setIdempotencyKey(idempotency);
        pedido.setTotal(carrinho.getValorTotal());
        pedido.setStatus(PedidoStatus.AGUARDANDO_PAGAMENTO);
        // O carrinho já reservou o estoque com lock pessimista.
        marcarEstoqueBaixado(pedido);

        // 3. Cria o snapshot dos itens — imutável após este momento
        List<ItemPedido> itensPedido = carrinho.getItens().stream()
                .map(item -> criarItemPedido(item, pedido))
                .collect(Collectors.toList());

        pedido.setItens(itensPedido);
        pedidoRepository.save(pedido);

        // 4. Marca carrinho como convertido e limpa os itens (já foram copiados para o pedido)
        carrinho.getItens().clear();
        carrinho.setValorTotal(BigDecimal.ZERO);
        carrinho.setCarrinhoStatus(CarrinhoStatus.CONVERTIDO);
        carrinho.setExpiresAt(null);
        carrinhoRepository.save(carrinho);

        // 5. Dispara evento de e-mail via RabbitMQ
        PedidoEvent event = new PedidoEvent(
                pedido.getId(),
                pedido.getUser().getId(),
                pedido.getTotal(),
                pedido.getUser().getEmail()
        );
        pedidoProducer.send(event);

        log.info("Checkout realizado: pedido={}, usuario={}", pedido.getId(), userId);

        return new PedidoCheckoutResponseDTO(
                pedido.getId(),
                pedido.getTotal()
        );
    }

    // ─── VENDA PRESENCIAL (balcão) ──────────────────────────────────

    @Transactional //tudo ou nada se um falhar todos falham
    public PedidoCheckoutResponseDTO criarVendaPresencial(UUID userId, VendaPresencialRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Produto produto = produtoRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ProductNotFound("Produto não encontrado"));

        int quantidade = request.quantity();

        // Baixar estoque se solicitado
        if (request.baixarEstoque()) {
            BigDecimal qtd = BigDecimal.valueOf(quantidade);
            if (produto.getEstoque().compareTo(qtd) < 0) {
                throw new QuantidadInvalid("Estoque insuficiente");
            }
            produto.setEstoque(produto.getEstoque().subtract(qtd));
            produtoRepository.save(produto);
        }

        // Calcular total
        BigDecimal total = produto.getPrice().multiply(BigDecimal.valueOf(quantidade));

        // Determinar status
        PedidoStatus status = request.confirmarVenda()
                ? PedidoStatus.PAGO
                : PedidoStatus.AGUARDANDO_PAGAMENTO;

        // Criar pedido
        Pedido pedido = new Pedido();
        pedido.setUser(user);
        pedido.setIdempotencyKey(UUID.randomUUID().toString());
        pedido.setTotal(total);
        pedido.setStatus(status);
        pedido.setFormaPagamento(request.paymentMethod());
        if (request.baixarEstoque()) {
            marcarEstoqueBaixado(pedido);
        }

        if (status == PedidoStatus.PAGO) {
            pedido.setDataPayment(LocalDateTime.now());
        }

        // Criar item do pedido
        ItemPedido item = new ItemPedido();
        item.setPedido(pedido);
        item.setProduto(produto);
        item.setNomeProduto(produto.getName());
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(produto.getPrice());
        item.setSubtotal(total);

        pedido.setItens(List.of(item));
        pedidoRepository.save(pedido);

        log.info("Venda presencial: pedido={}, produto={}, qtd={}, total={}, status={}",
                pedido.getId(), produto.getName(), quantidade, total, status);

        return new PedidoCheckoutResponseDTO(
                pedido.getId(),
                pedido.getTotal()
        );
    }

    public List<PedidoResumoDTO> getPedidosResumo(UUID userId) {
        return pedidoRepository.findByUserIdOrderByDataCriacaoDesc(userId)
                .stream()
                .map(PedidoResumoDTO::from)
                .toList();
    }

    public PedidoResumoDTO getPedidoById(UUID pedidoId, UUID userId) {
        Pedido pedido = pedidoRepository.findByIdAndUserId(pedidoId, userId)
                .orElseThrow(() -> new PaymentNotFound("Pedido não encontrado"));
        return PedidoResumoDTO.from(pedido);
    }

    @Transactional
    public PedidoResumoDTO baixarEstoque(
            UUID pedidoId,
            BaixaEstoqueRequestDTO request
    ) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PaymentNotFound("Pedido não encontrado"));

        // Idempotente: se já baixou, apenas garante o status final.
        if (pedido.isStockReleased()) {
            pedido.setStatus(PedidoStatus.PRONTO);
            return PedidoResumoDTO.from(pedidoRepository.save(pedido));
        }

        // Só permite baixar estoque de pedidos PAGO ou EM_SEPARACAO.
        if (pedido.getStatus() != PedidoStatus.PAGO
                && pedido.getStatus() != PedidoStatus.EM_SEPARACAO) {
            throw new IllegalArgumentException(
                    "Só é possível baixar estoque de pedidos em PAGO ou EM_SEPARACAO. Status atual: " + pedido.getStatus()
            );
        }

        Map<UUID, BigDecimal> quantidades = new LinkedHashMap<>();
        pedido.getItens().forEach(item -> {
            if (item.getProduto() == null) {
                throw new ProductNotFound("Item do pedido sem produto associado");
            }
            quantidades.merge(
                    item.getProduto().getId(),
                    BigDecimal.valueOf(item.getQuantidade()),
                    BigDecimal::add
            );
        });

        Map<UUID, Produto> produtos = new LinkedHashMap<>();
        quantidades.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Produto produto = produtoRepository.findByIdForUpdate(entry.getKey())
                            .orElseThrow(() -> new ProductNotFound("Produto não encontrado"));
                    BigDecimal solicitada = entry.getValue();
                    if (produto.getEstoque().compareTo(solicitada) < 0) {
                        throw new InsufficientStock(
                                "Estoque insuficiente para o produto " + produto.getName() + ".",
                                produto.getId(),
                                produto.getEstoque(),
                                solicitada
                        );
                    }
                    produtos.put(entry.getKey(), produto);
                });

        // Todas as validações ocorreram antes de qualquer alteração.
        quantidades.forEach((produtoId, quantidade) -> {
            Produto produto = produtos.get(produtoId);
            produto.retirarEstoque(quantidade);
            produtoRepository.save(produto);
        });

        marcarEstoqueBaixado(pedido);
        pedido.setStatus(PedidoStatus.PRONTO);
        return PedidoResumoDTO.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResumoDTO confirmarPagamento(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PaymentNotFound("Pedido não encontrado"));

        if (pedido.getStatus() == PedidoStatus.PAGO) {
            // Idempotente: já está pago, retorna sem alterar.
            return PedidoResumoDTO.from(pedido);
        }
        if (pedido.getStatus() != PedidoStatus.AGUARDANDO_PAGAMENTO) {
            throw new IllegalArgumentException(
                    "Só é possível confirmar pagamento de pedidos em AGUARDANDO_PAGAMENTO. Status atual: " + pedido.getStatus()
            );
        }

        pedido.setStatus(PedidoStatus.PAGO);
        pedido.setDataPayment(LocalDateTime.now());
        return PedidoResumoDTO.from(pedidoRepository.save(pedido));
    }

    private static final Map<PedidoStatus, List<PedidoStatus>> TRANSICOES_VALIDAS = Map.of(
            PedidoStatus.AGUARDANDO_PAGAMENTO, List.of(PedidoStatus.PAGO, PedidoStatus.CANCELADO),
            PedidoStatus.PAGO, List.of(PedidoStatus.EM_SEPARACAO, PedidoStatus.PRONTO, PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.EM_SEPARACAO, List.of(PedidoStatus.PRONTO, PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.PRONTO, List.of(PedidoStatus.ENVIADO, PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.ENVIADO, List.of(PedidoStatus.ENTREGUE, PedidoStatus.CANCELADO),
            PedidoStatus.ENTREGUE, List.of(),
            PedidoStatus.CANCELADO, List.of()
    );

    @Transactional
    public PedidoResumoDTO atualizarStatus(UUID pedidoId, PedidoStatus novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("Status do pedido é obrigatório");
        }
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new PaymentNotFound("Pedido não encontrado"));

        PedidoStatus statusAtual = pedido.getStatus();
        List<PedidoStatus> permitidos = TRANSICOES_VALIDAS.getOrDefault(statusAtual, List.of());

        if (!permitidos.contains(novoStatus)) {
            throw new IllegalArgumentException(
                    "Transição inválida: " + statusAtual + " → " + novoStatus
                            + ". Transições permitidas: " + permitidos
            );
        }

        log.info("Atualizando pedido {} de {} para {}", pedidoId, statusAtual, novoStatus);
        pedido.setStatus(novoStatus);
        pedidoRepository.save(pedido);
        return PedidoResumoDTO.from(pedido);
    }

    private void marcarEstoqueBaixado(Pedido pedido) {
        pedido.setStockReleased(true);
        pedido.setStockReleasedAt(LocalDateTime.now());
        pedido.setStockReleaseId(UUID.randomUUID());
    }

    /** Monta o snapshot do ItemCarrinho para ItemPedido. */
    private ItemPedido criarItemPedido(ItemCarrinho item, Pedido pedido) {
        // Usa snapshot do carrinho; fallback para produto se disponível
        String nomeProduto = item.getName() != null
                ? item.getName()
                : (item.getProduto() != null ? item.getProduto().getName() : "Produto");

        ItemPedido ip = new ItemPedido();
        ip.setPedido(pedido);
        ip.setProduto(item.getProduto());
        ip.setNomeProduto(nomeProduto);
        ip.setQuantidade(item.getQuantidade());
        ip.setPrecoUnitario(item.getPrice());
        ip.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantidade())));
        return ip;
    }
}
