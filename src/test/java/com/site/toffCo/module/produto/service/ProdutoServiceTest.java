package com.site.toffCo.module.produto.service;

import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ProdutoMapper produtoMapper;
    @InjectMocks
    private ProdutoService produtoService;

    @Test
    @DisplayName("Deve Criar um produto")
    void create() {
        ProdutoRequestDTO produtodto = new ProdutoRequestDTO("filamento", "descriçao de filamento", 80f, "filamento", "twste", 2);
        Produto produto = new Produto();
        produto.setName("filamento");
        produto.setDescription("descriçao de filamento");
        produto.setPrice(80f);
        produto.setCategoria("filamento");
        produto.setImage("twste");
        produto.setEstoque(2);

        ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO("filamento", "descriçao de filamento", "twste", 80f, 2);

        Mockito.when(produtoMapper.toEntity(produtodto)).thenReturn(produto);
        Mockito.when(produtoRepository.save(produto)).thenReturn(produto);
        Mockito.when(produtoMapper.toDto(produto)).thenReturn(produtoResponseDTO);

        ProdutoResponseDTO responseDTO = produtoService.create(produtodto);
        assertNotNull(responseDTO);
        assertEquals("filamento", responseDTO.name());
        assertEquals("descriçao de filamento", responseDTO.description());
        assertEquals(80f, responseDTO.price());
        assertEquals("twste", responseDTO.image());
        assertEquals(2, responseDTO.estoque());

        Mockito.verify(produtoMapper).toEntity(produtodto);
        Mockito.verify(produtoRepository, Mockito.times(1)).save(produto);
        Mockito.verify(produtoMapper).toDto(produto);
    }

    @Test
    void update() {
    }

    @Test
    void findAll() {
        Produto produto = new Produto();
        produto.setName("filamento");
        produto.setPrice(80f);

        ProdutoResponseDTO produtoResponseDTO = new ProdutoResponseDTO("filamento", "descriçao de filamento", "twste", 80f, 2);

        Mockito.when(produtoRepository.findAll()).thenReturn(List.of(produto));
        Mockito.when(produtoMapper.toDto(produto)).thenReturn(produtoResponseDTO);

        List<ProdutoResponseDTO> responseDTO = produtoService.findAll();
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.size());
        assertEquals("filamento", responseDTO.get(0).name());

        Mockito.verify(produtoRepository).findAll();
        Mockito.verify(produtoMapper).toDto(produto);
    }

    @Test
    void findById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void findByCategoriaId() {
    }

    @Test
    void findByDescricao() {
    }
}