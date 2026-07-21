package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
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
    public void realizarCheckout(String userId) {
        // 1. Busca o carrinho do usuário
        Carrinho carrinho = carrinhoRepository.findByUser_Id(UUID.fromString(userId))
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
        List<OrderItemOdooEvent> odooItems = carrinho.getItens().stream()
                .map(item -> new OrderItemOdooEvent(
                        item.getProduto().getId().toString(),
                        item.getQuantidade(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());

        var odooEvent = new OrderConfirmedOdooEvent(
                pedido.getId(),
                "234.342.234-21",
                pedido.getTotal(),
                odooItems
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_ODOO_INVOICE,
                odooEvent
        );

        // 6. Limpa o carrinho — esvazia, não deleta
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }

    /** Monta o snapshot do ItemCarrinho para ItemPedido. */
    private ItemPedido criarItemPedido(ItemCarrinho item, Pedido pedido) {
        ItemPedido ip = new ItemPedido();
        ip.setPedido(pedido);
        ip.setProduto(item.getProduto());
        ip.setNomeProduto(item.getName());
        ip.setQuantidade(item.getQuantidade());
        ip.setPrecoUnitario(item.getPrice());
        ip.setSubtotal(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantidade())));
        return ip;
    }
}
