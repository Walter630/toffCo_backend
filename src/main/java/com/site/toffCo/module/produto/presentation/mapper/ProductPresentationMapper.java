package com.site.toffCo.module.produto.presentation.mapper;

import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;

import java.util.List;

public class ProductPresentationMapper {
    public CreateProductCommand toCreateCommand(ProdutoRequestDTO request) {
        return request.toCommand();
    }

    public UpdateProductCommand toUpdateCommand(ProdutoRequestDTO request) {
        return new UpdateProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.image(),
                request.images() == null ? List.of() : List.copyOf(request.images()),
                request.featured(),
                request.categoria(),
                request.estoque(),
                request.type(),
                request.typePersonalizado(),
                request.marca(),
                request.peso(),
                request.diametro(),
                request.codigoBarras(),
                request.status()
        );
    }

    public ProdutoResponseDTO toResponse(Produto produto) {
        return new ProdutoResponseDTO(
                produto.getId(),
                produto.getName(),
                produto.getDescription(),
                produto.getImage(),
                produto.getImages(),
                produto.isFeatured(),
                produto.getCategoria(),
                produto.getMarca(),
                produto.getType(),
                produto.getTypePersonalizado(),
                produto.getPrice(),
                produto.getEstoque(),
                produto.getPeso(),
                produto.getDiametro(),
                produto.getCodigoBarras(),
                produto.getStatus(),
                produto.getCreatedAt(),
                produto.getUpdatedAt()
        );
    }
}
