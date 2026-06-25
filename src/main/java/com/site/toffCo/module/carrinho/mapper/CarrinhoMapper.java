package com.site.toffCo.module.carrinho.mapper;

import com.site.toffCo.module.carrinho.dto.CarrinhoRequestDTO;
import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.dto.CarrinhoResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CarrinhoMapper {
    public abstract Carrinho toEntity(CarrinhoRequestDTO carrinhoRequestDTO);
    public abstract CarrinhoResponseDTO toDto(Carrinho entity);
}
