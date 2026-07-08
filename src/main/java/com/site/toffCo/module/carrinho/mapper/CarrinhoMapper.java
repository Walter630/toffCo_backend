package com.site.toffCo.module.carrinho.mapper;

import com.site.toffCo.module.carrinho.dto.CarrinhoRequestDTO;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.dto.CarrinhoResponseDTO;
import com.site.toffCo.module.itemcarrinho.mapper.ItemCarrinhoMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ItemCarrinhoMapper.class})
public abstract class CarrinhoMapper {
    public abstract Carrinho toEntity(CarrinhoRequestDTO carrinhoRequestDTO);
    @Mapping(source = "itens", target = "items")
    public abstract CarrinhoResponseDTO toDto(Carrinho entity);
}
