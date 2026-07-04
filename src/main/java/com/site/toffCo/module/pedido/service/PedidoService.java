package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.rabbitMQ.RabbitMQConfig;
import com.site.toffCo.module.pedido.dto.OrderItemEvent;
import com.site.toffCo.module.pedido.dto.PedidoEvent;
import com.site.toffCo.infra.rabbitMQ.PedidoProducer;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.carrinho.service.CarrinhoService;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    // Construtor aqui...
    public PedidoService(PedidoRepository pedidoRepository, PedidoProducer pedidoProducer,
                         CarrinhoService carrinhoService,  CarrinhoRepository carrinhoRepository, RabbitTemplate rabbitTemplate) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoProducer = pedidoProducer;
        this.carrinhoService = carrinhoService;
        this.carrinhoRepository = carrinhoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void realizarCheckout(String userId) {
        // 1. Busca no banco
        Carrinho carrinho = carrinhoRepository.findByUser_Id(UUID.fromString(userId))
                .orElseThrow(() -> new CarNotFound("Carrinho não encontrado"));

        // 2. Validação
        if (carrinho.getItens().isEmpty()) throw new RuntimeException("Carrinho vazio!");

        // 3. Cria o pedido
        Pedido pedido = new Pedido();
        pedido.setUser(carrinho.getUser());
        pedido.setTotal(carrinho.getValorTotal()); // Você precisa criar esse método no Carrinho
        pedidoRepository.save(pedido);

        // 4. Dispara evento
        PedidoEvent event = new PedidoEvent(pedido.getId(), pedido.getUser().getId(), pedido.getTotal(), pedido.getUser().getEmail());
        pedidoProducer.send(event);

        // Odoo parte
        List<OrderItemOdooEvent> odooItems = carrinho.getItens().stream()
                .map(item -> new OrderItemOdooEvent(
                        item.getProduto().getId().toString(),
                        item.getQuantidade(),
                        item.getPrice()
                ))
                        .collect(Collectors.toList());

        //criamos o evento
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

        // 5. Limpa (cuidado: o carrinho deve ser esvaziado, não deletado)
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }
}
