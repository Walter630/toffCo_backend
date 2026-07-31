package com.site.toffCo.module.produto.presentation;

import com.site.toffCo.module.codigoBarras.service.CodigoBarrasServices;
import com.site.toffCo.module.produto.application.command.*;
import com.site.toffCo.module.produto.application.query.*;
import com.site.toffCo.module.produto.presentation.response.PageResponseDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.presentation.request.ProductQueryFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProductQueryController {

    private final FindAllProductUseCase findAllProductsUseCase;
    private final FindByProductUseCase findProductByIdUseCase;
    private final UploadImageProductUseCase uploadImageProductUseCase;
    private final CodigoBarrasServices codigoServices;

    //============================== LIST ==============================

    @GetMapping
    //modelAtribrute ele mapeia automaticamente os query params da url ex: /produtos?name=camisa&categoria=roupas
    public ResponseEntity<PageResponseDTO<ProdutoResponseDTO>> getAllProdutos(
            @ModelAttribute ProductQueryFilter filter,
            @PageableDefault(
                    size = 15,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable page
            ) {
        return ResponseEntity.ok(findAllProductsUseCase.findAll(filter, page));
    }

    //============================== FINDBYID ==============================

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(findProductByIdUseCase.findById(id));
    }

    //============================== LIST ==============================

    @GetMapping("/{id}/codigo-barras/imagem")
    public ResponseEntity<byte[]> getImagem(@PathVariable UUID id) {
        byte[] imagem = codigoServices.buscarImagemCodigoBarras(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imagem);
    }

    //============================== UPLOAD IMAGE ==============================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("image") MultipartFile image) {
        String url = uploadImageProductUseCase.uploadImage(image);
        return ResponseEntity.ok().body(Map.of("url", url));
    }
}
