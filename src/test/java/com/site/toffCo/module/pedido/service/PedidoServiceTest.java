package com.site.toffCo.module.pedido.service;

import com.site.toffCo.infra.exception.carrinho.CarIsEmpty;
import com.site.toffCo.infra.exception.carrinho.CarNotFound;
import com.site.toffCo.infra.rabbitMQ.PedidoProducer;
import com.site.toffCo.module.carrinho.dto.CarrinhoStatus;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.pedido.dto.PedidoCheckoutResponseDTO;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoProducer pedidoProducer;
    @Mock private CarrinhoRepository carrinhoRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private UserRepository userRepository;

    private PedidoService service;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, pedidoProducer, carrinhoRepository, produtoRepository, userRepository);

        user = new User();
        userId = UUID.randomUUID();
        user.setId(userId);
        user.setEmail("teste@test.com");
        user.setUsername("Teste");
    }

    @Test
    @DisplayName("Checkout deve criar pedido e marcar carrinho como CONVERTIDO")
    void checkout_deveCriarPedidoEMarcarCarrinhoConvertido() {
        Carrinho carrinho = criarCarrinhoComItem();
        when(carrinhoRepository.findByUser_Id(userId)).thenReturn(Optional.of(carrinho));
        when(pedidoRepository.findByUserIdAndIdempotencyKey(userId, "key-1"))
                .thenReturn(Optional.empty());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> {
            Pedido p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PedidoCheckoutResponseDTO response = service.realizarCheckout(userId, "key-1");

        assertThat(response).isNotNull();
        assertThat(response.valor()).isEqualTo(new BigDecimal("50.00"));
        assertThat(carrinho.getCarrinhoStatus()).isEqualTo(CarrinhoStatus.CONVERTIDO);
        assertThat(carrinho.getExpiresAt()).isNull();
        verify(carrinhoRepository).save(carrinho);
        verify(pedidoProducer).send(any());
    }

    @Test
    @DisplayName("Checkout com mesma chave deve retornar pedido existente sem criar novo")
    void checkout_mesmaChave_retornaPedidoExistente() {
        Pedido pedidoExistente = new Pedido();
        pedidoExistente.setId(UUID.randomUUID());
        pedidoExistente.setTotal(new BigDecimal("100.00"));

        when(pedidoRepository.findByUserIdAndIdempotencyKey(userId, "key-dup"))
                .thenReturn(Optional.of(pedidoExistente));

        PedidoCheckoutResponseDTO response = service.realizarCheckout(userId, "key-dup");

        assertThat(response.pedidoId()).isEqualTo(pedidoExistente.getId());
        verify(pedidoRepository, never()).save(any());
        verify(carrinhoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checkout de carrinho convertido deve ser rejeitado")
    void checkout_carrinhoConvertido_deveRejeitar() {
        Carrinho carrinho = criarCarrinhoComItem();
        carrinho.setCarrinhoStatus(CarrinhoStatus.CONVERTIDO);

        when(pedidoRepository.findByUserIdAndIdempotencyKey(userId, "key-2"))
                .thenReturn(Optional.empty());
        when(carrinhoRepository.findByUser_Id(userId)).thenReturn(Optional.of(carrinho));

        assertThatThrownBy(() -> service.realizarCheckout(userId, "key-2"))
                .isInstanceOf(CarIsEmpty.class);
    }

    @Test
    @DisplayName("Checkout de carrinho vazio deve ser rejeitado")
    void checkout_carrinhoVazio_deveRejeitar() {
        Carrinho carrinho = new Carrinho();
        carrinho.setUser(user);
        carrinho.setItens(new ArrayList<>());
        carrinho.setCarrinhoStatus(CarrinhoStatus.ABERTO);

        when(pedidoRepository.findByUserIdAndIdempotencyKey(userId, "key-3"))
                .thenReturn(Optional.empty());
        when(carrinhoRepository.findByUser_Id(userId)).thenReturn(Optional.of(carrinho));

        assertThatThrownBy(() -> service.realizarCheckout(userId, "key-3"))
                .isInstanceOf(CarIsEmpty.class);
    }

    @Test
    @DisplayName("Checkout sem carrinho deve lançar CarNotFound")
    void checkout_semCarrinho_deveRejeitar() {
        when(pedidoRepository.findByUserIdAndIdempotencyKey(userId, "key-4"))
                .thenReturn(Optional.empty());
        when(carrinhoRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.realizarCheckout(userId, "key-4"))
                .isInstanceOf(CarNotFound.class);
    }

    private Carrinho criarCarrinhoComItem() {
        Produto produto = new Produto();
        produto.setId(UUID.randomUUID());
        produto.setName("Filamento PLA");
        produto.setPrice(new BigDecimal("50.00"));

        ItemCarrinho item = new ItemCarrinho();
        item.setId(UUID.randomUUID());
        item.setProduto(produto);
        item.setName("Filamento PLA");
        item.setQuantidade(1);
        item.setPrice(new BigDecimal("50.00"));

        Carrinho carrinho = new Carrinho();
        carrinho.setId(UUID.randomUUID());
        carrinho.setUser(user);
        carrinho.setValorTotal(new BigDecimal("50.00"));
        carrinho.setCarrinhoStatus(CarrinhoStatus.ABERTO);
        carrinho.setItens(new ArrayList<>(List.of(item)));
        item.setCarrinho(carrinho);

        return carrinho;
    }
}
