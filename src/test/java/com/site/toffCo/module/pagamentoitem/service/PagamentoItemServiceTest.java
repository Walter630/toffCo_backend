package com.site.toffCo.module.pagamentoitem.service;

import com.site.toffCo.infra.exception.payment.PaymentInvalidForm;
import com.site.toffCo.infra.exception.payment.PaymentNotFound;
import com.site.toffCo.infra.outbox.OutboxEventRepository;
import com.site.toffCo.infra.rabbitMQ.EmailService;
import com.site.toffCo.infra.utils.AuthUtil;
import com.site.toffCo.module.whatzap.service.WhatzapService;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoRequestDTO;
import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoStatus;
import com.site.toffCo.module.pagamentoitem.repository.PagamentoItemRepository;
import com.site.toffCo.module.pagamentoitem.strategy.PagamentoStrategy;
import com.site.toffCo.module.pedido.entity.Pedido;
import com.site.toffCo.module.pedido.entity.PedidoStatus;
import com.site.toffCo.module.pedido.repository.PedidoRepository;
import com.site.toffCo.module.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoItemServiceTest {

    @Mock private PagamentoItemRepository pagamentoItemRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private AuthUtil authUtil;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private EmailService emailService;
    @Mock private WhatzapService whatzapService;
    @Mock private PagamentoStrategy pixStrategy;
    @Mock private org.thymeleaf.TemplateEngine templateEngine;

    private PagamentoItemService service;
    private User user;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        when(pixStrategy.getTipoPagamento()).thenReturn("PIX");

        service = new PagamentoItemService(
                List.of(pixStrategy),
                pagamentoItemRepository,
                pedidoRepository,
                authUtil,
                outboxEventRepository,
                emailService,
                whatzapService,
                templateEngine
        );

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("cliente@test.com");
        user.setUsername("Cliente Teste");

        pedido = new Pedido();
        pedido.setId(UUID.randomUUID());
        pedido.setUser(user);
        pedido.setTotal(new BigDecimal("150.00"));
        pedido.setStatus(PedidoStatus.AGUARDANDO_PAGAMENTO);
        pedido.setItens(List.of());
    }

    @Test
    @DisplayName("Pagamento aprovado deve alterar status do pedido para PAGO")
    void pagamentoAprovado_deveMarcarPedidoComoPago() {
        when(authUtil.getUserLogado()).thenReturn(user);
        when(pedidoRepository.findByIdAndUserId(pedido.getId(), user.getId()))
                .thenReturn(Optional.of(pedido));
        when(pagamentoItemRepository.findByPedidoIdAndStatus(pedido.getId(), PagamentoStatus.APROVADO))
                .thenReturn(Optional.empty());
        when(pixStrategy.processar(any(), any()))
                .thenReturn(new ResponseDTO("PIX", "PAGO", null, null, "Pagamento aprovado"));

        var request = new PagamentoRequestDTO("PIX", new BigDecimal("150.00"), pedido.getId());
        ResponseDTO response = service.getPagamentoItem(request);

        assertThat(response.status()).isEqualTo("PAGO");
        assertThat(pedido.getStatus()).isEqualTo(PedidoStatus.PAGO);
        verify(pedidoRepository).save(pedido);
        verify(pagamentoItemRepository).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar pagamento de pedido de outro usuario")
    void pagamento_pedidoDeOutroUsuario_deveRejeitar() {
        when(authUtil.getUserLogado()).thenReturn(user);
        when(pedidoRepository.findByIdAndUserId(any(), eq(user.getId())))
                .thenReturn(Optional.empty());

        var request = new PagamentoRequestDTO("PIX", new BigDecimal("100.00"), UUID.randomUUID());

        assertThatThrownBy(() -> service.getPagamentoItem(request))
                .isInstanceOf(PaymentNotFound.class);
    }

    @Test
    @DisplayName("Deve rejeitar pagamento com valor diferente do pedido")
    void pagamento_valorDiferente_deveRejeitar() {
        when(authUtil.getUserLogado()).thenReturn(user);
        when(pedidoRepository.findByIdAndUserId(pedido.getId(), user.getId()))
                .thenReturn(Optional.of(pedido));
        when(pagamentoItemRepository.findByPedidoIdAndStatus(pedido.getId(), PagamentoStatus.APROVADO))
                .thenReturn(Optional.empty());

        var request = new PagamentoRequestDTO("PIX", new BigDecimal("1.00"), pedido.getId());

        assertThatThrownBy(() -> service.getPagamentoItem(request))
                .isInstanceOf(PaymentInvalidForm.class)
                .hasMessageContaining("valor");
    }

    @Test
    @DisplayName("Deve rejeitar pagamento de pedido ja pago")
    void pagamento_pedidoJaPago_deveRejeitar() {
        pedido.setStatus(PedidoStatus.PAGO);
        when(authUtil.getUserLogado()).thenReturn(user);
        when(pedidoRepository.findByIdAndUserId(pedido.getId(), user.getId()))
                .thenReturn(Optional.of(pedido));

        var request = new PagamentoRequestDTO("PIX", new BigDecimal("150.00"), pedido.getId());

        assertThatThrownBy(() -> service.getPagamentoItem(request))
                .isInstanceOf(PaymentInvalidForm.class)
                .hasMessageContaining("aguardando");
    }

    @Test
    @DisplayName("Deve rejeitar pagamento duplicado aprovado")
    void pagamento_duplicadoAprovado_deveRejeitar() {
        when(authUtil.getUserLogado()).thenReturn(user);
        when(pedidoRepository.findByIdAndUserId(pedido.getId(), user.getId()))
                .thenReturn(Optional.of(pedido));
        when(pagamentoItemRepository.findByPedidoIdAndStatus(pedido.getId(), PagamentoStatus.APROVADO))
                .thenReturn(Optional.of(new com.site.toffCo.module.pagamentoitem.entity.PagamentoItem()));

        var request = new PagamentoRequestDTO("PIX", new BigDecimal("150.00"), pedido.getId());

        assertThatThrownBy(() -> service.getPagamentoItem(request))
                .isInstanceOf(PaymentInvalidForm.class)
                .hasMessageContaining("aprovado");
    }

    @Test
    @DisplayName("Deve rejeitar forma de pagamento invalida")
    void pagamento_formaInvalida_deveRejeitar() {
        var request = new PagamentoRequestDTO("BITCOIN", new BigDecimal("100.00"), UUID.randomUUID());

        assertThatThrownBy(() -> service.getPagamentoItem(request))
                .isInstanceOf(PaymentInvalidForm.class)
                .hasMessageContaining("suportado");
    }
}
