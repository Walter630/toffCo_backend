package com.site.toffCo.module.produto.presentation;

import com.site.toffCo.module.produto.application.command.CreateProductUseCase;
import com.site.toffCo.module.produto.application.command.DeleteProductUseCase;
import com.site.toffCo.module.produto.application.command.UpdateProdutctUseCase;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.request.UpdateProductDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProductCommandController {
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProdutctUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;

    //============================== CREATE ==============================

    @PostMapping()
    public ResponseEntity<ProdutoResponseDTO> Create(@Valid @RequestBody ProdutoRequestDTO produtoRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createProductUseCase.create(produtoRequestDTO.toCommand()));
    }

    //============================== UPDATE ==============================

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> Update(@PathVariable UUID id, @Valid @RequestBody UpdateProductDTO updateProductDTO) {
        return ResponseEntity.ok().body(updateProductUseCase.update(id, updateProductDTO.toCommand()));
    }

    //============================== DELETE ==============================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> Delete(@PathVariable UUID id) {
        deleteProductUseCase.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
