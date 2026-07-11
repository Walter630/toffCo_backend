package com.site.toffCo.module.produto.service;

import com.google.zxing.WriterException;
import com.site.toffCo.infra.exception.product.ProductNotFound;
import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.mapper.ProdutoMapper;
import com.site.toffCo.module.produto.queryFilter.ProductQueryFilter;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import com.site.toffCo.module.user.entity.Role;
import com.site.toffCo.module.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    @Value("${app.upload-dir:/app/upload}")
    private String uploadDir;

    private final ProdutoRepository repository;
    private final ProdutoMapper mapper;
    private final UserRepository userService;
    private final CodigoBarrasServices codigoBarrasServices;

    //============================== CREATE PRODUCT ==============================

    @Transactional
    public ProdutoResponseDTO create(ProdutoRequestDTO produtoDTO) {
        Produto produto = mapper.toEntity(produtoDTO);
        produto = repository.save(produto);
        log.info("Produto criado com id={}", produto.getId());

        String codigo = produto.getCodigoBarras();
        if (codigo == null || codigo.isBlank()) {
            codigo = codigoBarrasServices.gerarCodigoEAN13(produto.getId());
            produto.setCodigoBarras(codigo);
        }
        log.info("Codigo de barras gerado: {}", codigo);

        try{
            produto.setImagemCodigoBarras(codigoBarrasServices.gerarImagemCodigoBarras(codigo));
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Erro ao gerar imagem do codigo de barras", e);
        }

        return mapper.toDto(repository.save(produto));
    }

    //============================== UPDATE ==============================

    @Transactional
    public ProdutoResponseDTO update(UUID id, ProdutoRequestDTO produtoDTO) {
        Produto produto = repository.findById(id).orElseThrow(() ->
                new ProductNotFound("Not found with id " + id));
        mapper.toUpdateEntity(produtoDTO, produto);
        log.info("Produto atualizado: {}", produto);
        return mapper.toDto(repository.save(produto));
    }

    //============================== LIST ==============================

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> findAll(ProductQueryFilter filter) {
        Specification<Produto> specification = filter.buildSpecification();
        List<Produto> produtos = repository.findAll(specification);
        List<ProdutoResponseDTO> dtos = mapper.toDto(produtos);
        System.out.println("Qtd de DTOs mapeados para JSON: " + dtos.size());
        if (!dtos.isEmpty()) {
            System.out.println("Primeiro DTO: " + dtos.get(0).name());
        }
        log.info("Produtos encontrados no repository: {}", produtos.size());
        return dtos;
    }

    //============================== FINDBYID ==============================

    @Transactional(readOnly = true)
    public ProdutoResponseDTO findById(UUID id) {
        log.info("Produto encontrado: {}", id);
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ProductNotFound("Product not found with id " + id)));
    }

    //============================== DELETE ==============================

    @Transactional
    public void deleteById(UUID id) {
        if(!repository.existsById(id)) {
            log.error("Produto nao encontrado");
            throw new ProductNotFound("Product not found with id " + id);
        }
        if (userService.findByRole(Role.ADMIN)) {
            //deleta somente se for admin
            repository.deleteById(id);
            log.info("Produto deletado: {}", id);
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Imagem vazia");
        }

        String contentType = image.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Arquivo enviado nao e uma imagem");
        }

        try {
            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/jpeg" -> ".jpg";
                case "image/webp" -> ".webp";
                default -> throw new IllegalArgumentException("Formato de imagem invalido");
            };

            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename).normalize();

            image.transferTo(destination);

            return "/uploads/" + filename;
        } catch (IOException exception) {
            throw new RuntimeException("Nao foi possivel salvar a imagem", exception);
        }
    }

}
