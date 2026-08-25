package com.site.toffCo.module.produto.presentation.mapper;

import com.site.toffCo.module.produto.application.command.model.CreateProductCommand;
import com.site.toffCo.module.produto.application.command.model.UpdateProductCommand;
import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ProdutoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imagemCodigoBarras", ignore = true)
    @Mapping(target = "odooProductId", ignore = true)
    @Mapping(target = "version", ignore = true)
    public abstract Produto toEntity(CreateProductCommand command);

    public abstract ProdutoResponseDTO toDto(Produto entity);

    public abstract List<ProdutoResponseDTO> toDto(List<Produto> entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imagemCodigoBarras", ignore = true)
    @Mapping(target = "odooProductId", ignore = true)
    @Mapping(target = "version", ignore = true)
    public abstract void toUpdateEntity(
            UpdateProductCommand command,
            @MappingTarget Produto produto
    );
}
