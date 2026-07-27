package com.site.toffCo.module.produto.service;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.CreateProductUseCase;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.presentation.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.infrastructure.persistence.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import com.site.toffCo.module.odoo.event.ProductChangedEvent;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ProdutoMapper produtoMapper;
    @Mock
    private CodigoBarrasServices  codigoBarrasServices;
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    private CreateProductUseCase createProduct;

    UUID id = UUID.randomUUID();

    ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO(id,"filamento", "descriçao de filamento", "twste","FILAMENTOS", BigDecimal.valueOf(80), BigDecimal.valueOf(2), ProductType.ABS, "ABS", ProductStatus.ATIVO, "212233333", "PLA");

    ProdutoRequestDTO produtodto = new ProdutoRequestDTO("filamento", "descriçao de filamento", BigDecimal.valueOf(80),"FILAMENTOS", "" , BigDecimal.valueOf(1), "32323232332", ProductType.ABS_PLUS, "ABS",  ProductStatus.ATIVO, "PLA");

    @Test
    @DisplayName("Deve Criar um produto")
    void create() throws Exception {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setName("filamento");
        produto.setDescription("descrição de filamento");
        produto.setPrice(BigDecimal.valueOf(80.99));
        produto.setCategoria("filamento");
        produto.setImage("twste");
        produto.setEstoque(BigDecimal.ONE);

        Mockito.when(produtoMapper.toEntity(produtodto))
                .thenReturn(produto);

        Mockito.when(produtoRepository.save(produto))
                .thenReturn(produto);

        Mockito.when(
                codigoBarrasServices.gerarCodigoEAN13(id)
        ).thenReturn("7896983662679");

        Mockito.when(
                codigoBarrasServices.gerarImagemCodigoBarras("7896983662679")
        ).thenReturn(new byte[]{1, 2, 3});

        Mockito.when(produtoMapper.toDto(produto))
                .thenReturn(produtoResponseDTO);

        ProdutoResponseDTO responseDTO =
                createProduct.create(produtodto);

        assertNotNull(responseDTO);
        assertEquals("filamento", responseDTO.name());
        assertEquals(
                "descriçao de filamento",
                responseDTO.description()
        );
        assertEquals(
                BigDecimal.valueOf(80),
                responseDTO.price()
        );
        assertEquals("twste", responseDTO.image());
        assertEquals(2, responseDTO.estoque());

        Mockito.verify(produtoMapper)
                .toEntity(produtodto);

        Mockito.verify(
                produtoRepository,
                Mockito.times(2)
        ).save(produto);

        Mockito.verify(codigoBarrasServices)
                .gerarCodigoEAN13(id);

        Mockito.verify(codigoBarrasServices)
                .gerarImagemCodigoBarras("7896983662679");

        Mockito.verify(publisher)
                .publishEvent(any(ProductChangedEvent.class));

        Mockito.verify(produtoMapper)
                .toDto(produto);
    }

    @Test
    void update(

    ) {
    }



    @Test
    void findById() {

    }

    @Test
    void deleteById() {
    }

}