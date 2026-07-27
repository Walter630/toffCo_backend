package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.odoo.dto.OdooInvoiceCreateDTO;
import com.site.toffCo.module.odoo.dto.OdooInvoiceLineDTO;
import com.site.toffCo.module.pedido.dto.PedidoCheckoutResponseDTO;
import com.site.toffCo.module.pedido.dto.PedidoEvent;
import com.site.toffCo.infra.rabbitMQ.PedidoProducer;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.carrinho.service.CarrinhoService;
import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.pedido.entity.ItemPedido;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoProducer pedidoProducer;
    private final CarrinhoService carrinhoService;
    private final CarrinhoRepository carrinhoRepository;
    private final RabbitTemplate rabbitTemplate;

    public PedidoService(PedidoRepository pedidoRepository, PedidoProducer pedidoProducer,
                         CarrinhoService carrinhoService, CarrinhoRepository carrinhoRepository,
                         RabbitTemplate rabbitTemplate) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoProducer = pedidoProducer;
        this.carrinhoService = carrinhoService;
        this.carrinhoRepository = carrinhoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public PedidoCheckoutResponseDTO realizarCheckout(UUID userId) {
        // 1. Busca o carrinho do usuário
        Carrinho carrinho = carrinhoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new CarNotFound("Carrinho não encontrado"));

        if (carrinho.getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio!");
        }

        // 2. Cria o pedido base
        Pedido pedido = new Pedido();
        pedido.setUser(carrinho.getUser());
        pedido.setTotal(carrinho.getValorTotal());
        pedido.setStatus(PedidoStatus.AGUARDANDO_PAGAMENTO);

        // 3. Cria o snapshot dos itens — imutável após este momento
        List<ItemPedido> itensPedido = carrinho.getItens().stream()
                .map(item -> criarItemPedido(item, pedido))
                .collect(Collectors.toList());

        pedido.setItens(itensPedido);
        pedidoRepository.save(pedido);

        // 4. Dispara evento de e-mail via RabbitMQ
        PedidoEvent event = new PedidoEvent(
                pedido.getId(),
                pedido.getUser().getId(),
                pedido.getTotal(),
                pedido.getUser().getEmail()
        );
        pedidoProducer.send(event);

        // 5. Dispara evento para o Odoo (nota fiscal)
        List<OdooInvoiceLineDTO> odooLines = carrinho.getItens().stream()
                .map(item -> new OdooInvoiceLineDTO(
                        item.getName(),
                        item.getQuantidade(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());

        /*
         * CPF pode ser nulo se o usuário ainda não cadastrou.
         * O Odoo aceita fatura sem CPF (emite como consumidor final).
         */
        var odooEvent = new OdooInvoiceCreateDTO(
                pedido.getId(),
                pedido.getUser().getUsername(),
                pedido.getUser().getCpf(),
                pedido.getUser().getEmail(),
                odooLines
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_INVOICE,
                odooEvent
        );

        return new PedidoCheckoutResponseDTO(
                pedido.getId(),
                pedido.getTotal()
        );
    }

    /** Monta o snapshot do ItemCarrinho para ItemPedido. */
    private ItemPedido criarItemPedido(ItemCarrinho item, Pedido pedido) {
        // Resolve o nome com fallback: campo snapshot do carrinho → nome do produto
        String nomeProduto = item.getName() != null
                ? item.getName()
                : (item.getProduto() != null ? item.getProduto().getName() : "Produto");

        ItemPedido ip = new ItemPedido();
        ip.setPedido(pedido);
        ip.setProduto(item.getProduto());
        ip.setNomeProduto(nomeProduto);
        ip.setQuantidade(item.getQuantidade());
        ip.setPrecoUnitario(item.getPrice());
        ip.setSubtotal(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantidade())));
        return ip;
    }
}
