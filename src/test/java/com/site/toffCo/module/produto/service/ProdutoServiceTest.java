package com.site.toffCo.module.produto.service;

import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
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
import org.springframework.data.jpa.domain.Specification;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ProdutoMapper produtoMapper;
    @InjectMocks
    private ProdutoService produtoService;

    UUID id = UUID.randomUUID();

    ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO(id,"filamento", "descriçao de filamento", "twste", BigDecimal.valueOf(80), 2);

    ProdutoRequestDTO produtodto = new ProdutoRequestDTO("filamento", "descriçao de filamento", BigDecimal.valueOf(80), "filamento", "twste", 2);

    @Test
    @DisplayName("Deve Criar um produto")
    void create() {

        Produto produto = new Produto();
        produto.setName("filamento");
        produto.setDescription("descriçao de filamento");
        produto.setPrice(BigDecimal.valueOf(80.99));
        produto.setCategoria("filamento");
        produto.setImage("twste");
        produto.setEstoque(2);

        Mockito.when(produtoMapper.toEntity(produtodto)).thenReturn(produto);
        Mockito.when(produtoRepository.save(produto)).thenReturn(produto);
        Mockito.when(produtoMapper.toDto(produto)).thenReturn(produtoResponseDTO);

        ProdutoResponseDTO responseDTO = produtoService.create(produtodto);
        assertNotNull(responseDTO);
        assertEquals("filamento", responseDTO.name());
        assertEquals("descriçao de filamento", responseDTO.description());
        assertEquals(BigDecimal.valueOf(80), responseDTO.price());
        assertEquals("twste", responseDTO.image());
        assertEquals(2, responseDTO.estoque());

        Mockito.verify(produtoMapper).toEntity(produtodto);
        Mockito.verify(produtoRepository, Mockito.times(1)).save(produto);
        Mockito.verify(produtoMapper).toDto(produto);
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

        ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO(id,"filamento", "descriçao de filamento", "twste", BigDecimal.valueOf(80), 2);

        Mockito.when(produtoRepository.findAll(any(Specification.class))).thenReturn(List.of(produto));
        Mockito.when(produtoMapper.toDto(produto)).thenReturn(produtoResponseDTO);

        List<ProdutoResponseDTO> responseDTO = produtoService.findAll(filter);

        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.size());
        assertEquals("filamento", responseDTO.get(0).name());

        Mockito.verify(produtoRepository).findAll(any(Specification.class));
        Mockito.verify(produtoMapper).toDto(produto);
    }

    @Test
    void findById() {
    }

    @Test
    void deleteById() {
    }

}