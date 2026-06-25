package com.site.toffCo.module.itemcarrinho.mapper;

import com.site.toffCo.module.itemcarrinho.entity.ItemCarrinho;
import com.site.toffCo.module.itemcarrinho.dto.ItemCarrinhoResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ItemCarrinhoMapper {
    public abstract ItemCarrinhoResponseDTO toDto(ItemCarrinho entity);
}
