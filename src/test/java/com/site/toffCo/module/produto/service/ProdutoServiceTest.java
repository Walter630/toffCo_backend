package com.site.toffCo.module.produto.service;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.dto.Status;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.queryFilter.ProductQueryFilter;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private ProdutoService produtoService;

    UUID id = UUID.randomUUID();

    ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO(id,"filamento", "descriçao de filamento", "twste","FILAMENTOS", BigDecimal.valueOf(80), 2, "", Status.ATIVO, "212233333", "PLA");

    ProdutoRequestDTO produtodto = new ProdutoRequestDTO("filamento", "descriçao de filamento", BigDecimal.valueOf(80),"FILAMENTOS", "" , BigDecimal.valueOf(1), "32323232332", "te",  Status.ATIVO, "PLA");

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
                produtoService.create(produtodto);

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
    void findAll() {
        ProductQueryFilter filter = new ProductQueryFilter();

        Produto produto = new Produto();
        produto.setName("filamento");
        produto.setPrice(BigDecimal.valueOf(80.99));

        ProdutoResponseDTO responseEsperado =
                new ProdutoResponseDTO(
                        id,
                        "filamento",
                        "descriçao de filamento",
                        "twste",
                        "FILAMENTOS",
                        BigDecimal.valueOf(80),
                        2,
                        "",
                        Status.ATIVO,
                        "212233333",
                        "PLA"
                );

        List<Produto> produtos = List.of(produto);
        List<ProdutoResponseDTO> dtos =
                List.of(responseEsperado);

        Mockito.when(
                produtoRepository.findAll(any(Specification.class))
        ).thenReturn(produtos);

        Mockito.when(
                produtoMapper.toDto(produtos)
        ).thenReturn(dtos);

        List<ProdutoResponseDTO> responseDTO =
                produtoService.findAll(filter);

        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.size());
        assertEquals(
                "filamento",
                responseDTO.getFirst().name()
        );

        Mockito.verify(produtoRepository)
                .findAll(any(Specification.class));

        Mockito.verify(produtoMapper)
                .toDto(produtos);
    }

    @Test
    void findById() {

    }

    @Test
    void deleteById() {
    }

}