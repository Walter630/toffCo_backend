package com.site.toffCo.module.produto.mapper;

import com.site.toffCo.module.produto.dto.ProdutoRequestDTO;
import com.site.toffCo.module.produto.dto.ProdutoResponseDTO;
import com.site.toffCo.module.produto.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ProdutoMapper {
     // Ignora o ID (o banco que gera)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Ignora campos de auditoria
    @Mapping(target = "updatedAt", ignore = true) // Ignora campos de auditoria
    public abstract Produto toEntity(ProdutoRequestDTO dto);

    public abstract ProdutoResponseDTO toDto(Produto entity);

    public abstract List<ProdutoResponseDTO> toDto(List<Produto> entity);
    // Diz ao MapStruct para pegar os campos do DTO (source = "dto")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "description", source = "dto.description")
    @Mapping(target = "price", source = "dto.price")
    @Mapping(target = "image", source = "dto.image")
    @Mapping(target = "status", source = "dto.status")
    public abstract void toUpdateEntity(ProdutoRequestDTO dto, @MappingTarget Produto produto);
}
