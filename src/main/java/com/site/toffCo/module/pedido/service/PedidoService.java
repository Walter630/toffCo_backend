package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.module.pedido.dto.PedidoEvent;
import com.site.toffCo.infra.rabbitMQ.PedidoProducer;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.carrinho.service.CarrinhoService;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import jakarta.transaction.Transactional;

import java.util.UUID;

public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final PedidoProducer pedidoProducer;
    private final CarrinhoService carrinhoService;
    private final CarrinhoRepository carrinhoRepository;

    // Construtor aqui...
    public PedidoService(PedidoRepository pedidoRepository, PedidoProducer pedidoProducer,
                         CarrinhoService carrinhoService,  CarrinhoRepository carrinhoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoProducer = pedidoProducer;
        this.carrinhoService = carrinhoService;
        this.carrinhoRepository = carrinhoRepository;
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

        // 5. Limpa (cuidado: o carrinho deve ser esvaziado, não deletado)
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }
}
