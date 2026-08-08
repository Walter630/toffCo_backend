package com.site.toffCo.module.produto.presentation.mapper;

import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;

public class ProductPresentationMapper {
    public CreateProductCommand toCreateCommand(
            ProdutoRequestDTO request
    ) {
        return new CreateProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.image(),
                request.categoria(),
                request.estoque(),
                request.type(),
                request.typePersonalizado(),
                request.marca(),
                request.peso(),
                request.diametro(),
                request.status()
        );
    }

    public UpdateProductCommand toUpdateCommand(
            ProdutoRequestDTO request
    ) {
        return new UpdateProductCommand(
                request.name(),
                request.description(),
                request.price(),
                request.image(),
                request.categoria(),
                request.estoque(),
                request.type(),
                request.typePersonalizado(),
                request.marca(),
                request.peso(),
                request.diametro(),
                request.status()
        );
    }

    public ProdutoResponseDTO toResponse(Produto produto) {
        return new ProdutoResponseDTO(
                produto.getId(),
                produto.getName(),
                produto.getDescription(),
                produto.getImage(),
                produto.getCategoria(),
                produto.getPrice(),
                produto.getEstoque(),
                produto.getType(),
                produto.getPeso(),
                produto.getDiametro(),
                produto.getTypePersonalizado(),
                produto.getStatus(),
                produto.getCodigoBarras(),
                produto.getMarca()
        );
    }
}
