package com.site.toffCo.module.produto.presentation;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.*;
import com.site.toffCo.module.produto.application.query.*;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.presentation.request.ProductQueryFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ProdutoController {

    private final FindAllProductUseCase findAllProductsUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProdutctUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final FindByProductUseCase findProductByIdUseCase;
    private final UploadImageProductUseCase uploadImageProductUseCase;
    private final CodigoBarrasServices codigoServices;

    //============================== LIST ==============================

    @GetMapping
    //modelAtribrute ele mapeia automaticamente os query params da url ex: /produtos?name=camisa&categoria=roupas
    public ResponseEntity<List<ProdutoResponseDTO>> GetAllProdutos(@ModelAttribute ProductQueryFilter filter) {
        return ResponseEntity.ok().body(findAllProductsUseCase.findAll(filter));
    }

    //============================== CREATE ==============================

    @PostMapping("/create")
    public ResponseEntity<ProdutoResponseDTO> Create(@Valid @RequestBody ProdutoRequestDTO produtoRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createProductUseCase.create(produtoRequestDTO));
    }

    //============================== UPDATE ==============================

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> Update(@PathVariable UUID id, @Valid @RequestBody ProdutoRequestDTO produtoRequestDTO) {
        return ResponseEntity.ok().body(updateProductUseCase.update(id, produtoRequestDTO));
    }

    //============================== DELETE ==============================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> Delete(@PathVariable UUID id) {
        deleteProductUseCase.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    //============================== FINDBYID ==============================

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> GetById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(findProductByIdUseCase.findById(id));
    }

    //============================== LIST ==============================

    @GetMapping("/{id}/codigo-barras/imagem")
    public ResponseEntity<byte[]> GetImagem(@PathVariable UUID id) {
        byte[] imagem = codigoServices.buscarImagemCodigoBarras(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imagem);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> Upload(@RequestPart("image") MultipartFile image) {
        String url = uploadImageProductUseCase.uploadImage(image);
        return ResponseEntity.ok().body(Map.of("url", url));
    }
}
