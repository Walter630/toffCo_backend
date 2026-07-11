package com.site.toffCo.module.produto.controller;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.queryFilter.ProductQueryFilter;
import com.site.toffCo.module.produto.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final CodigoBarrasServices codigoServices;

    public ProdutoController(ProdutoService service, CodigoBarrasServices codigoServices) {
        this.service = service;
        this.codigoServices = codigoServices;
    }

    //============================== LIST ==============================

    @GetMapping
    //modelAtribrute ele mapeia automaticamente os query params da url ex: /produtos?name=camisa&categoria=roupas
    public ResponseEntity<List<ProdutoResponseDTO>> GetAllProdutos(@ModelAttribute ProductQueryFilter filter) {
        return ResponseEntity.ok().body(service.findAll(filter));
    }

    //============================== CREATE ==============================

    @PostMapping("/create")
    public ResponseEntity<ProdutoResponseDTO> create(@Valid @RequestBody ProdutoRequestDTO produtoRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(produtoRequestDTO));
    }

    //============================== UPDATE ==============================

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody ProdutoRequestDTO produtoRequestDTO) {
        return ResponseEntity.ok().body(service.update(id, produtoRequestDTO));
    }

    //============================== DELETE ==============================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //============================== FINDBYID ==============================

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(service.findById(id));
    }

    //============================== LIST ==============================

    @GetMapping("/{id}/codigo-barras/imagem")
    public ResponseEntity<byte[]> getImagem(@PathVariable UUID id) {
        byte[] imagem = codigoServices.buscarImagemCodigoBarras(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imagem);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("image") MultipartFile image) {
        String url = service.uploadImage(image);
    }
}
