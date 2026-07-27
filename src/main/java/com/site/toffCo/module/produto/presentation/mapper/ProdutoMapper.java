package com.site.toffCo.module.produto.presentation.mapper;

import com.site.toffCo.module.produto.presentation.request.ProdutoRequestDTO;
import com.site.toffCo.module.produto.presentation.response.ProdutoResponseDTO;
import com.site.toffCo.module.produto.domain.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ProdutoMapper {
     // Ignora o ID (o banco que gera)
     @Mapping(target = "id", ignore = true)
     @Mapping(target = "ativo", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "updatedAt", ignore = true)
     @Mapping(target = "codigoBarras", ignore = true)
     @Mapping(target = "imagemCodigoBarras", ignore = true)
     @Mapping(target = "odooProductId", ignore = true)
    public abstract Produto toEntity(ProdutoRequestDTO dto);

    public abstract ProdutoResponseDTO toDto(Produto entity);

    public abstract List<ProdutoResponseDTO> toDto(List<Produto> entity);
    // Diz ao MapStruct para pegar os campos do DTO (source = "dto")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "codigoBarras", ignore = true)
    @Mapping(target = "imagemCodigoBarras", ignore = true)
    @Mapping(target = "odooProductId", ignore = true)
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "price", source = "dto.price")
    @Mapping(target = "image", source = "dto.image")
    @Mapping(target = "status", source = "dto.status")
    public abstract void toUpdateEntity(ProdutoRequestDTO dto, @MappingTarget Produto produto);
}
