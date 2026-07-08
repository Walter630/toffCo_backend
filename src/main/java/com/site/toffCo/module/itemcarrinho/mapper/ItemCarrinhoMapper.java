package com.site.toffCo.module.itemcarrinho.mapper;

import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.itemcarrinho.dto.ItemCarrinhoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ItemCarrinhoMapper {
    @Mapping(source = "produto.id", target = "produtoId")
    @Mapping(source = "produto.name", target = "name")
    @Mapping(source = "price", target = "price")
    public abstract ItemCarrinhoResponseDTO toDto(ItemCarrinho entity);
}
