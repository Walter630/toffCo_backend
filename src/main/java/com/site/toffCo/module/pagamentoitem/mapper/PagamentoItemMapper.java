package com.site.toffCo.module.pagamentoitem.mapper;

import com.site.toffCo.module.pagamentoitem.dto.ResponseDTO;
import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class PagamentoItemMapper {
    public abstract ResponseDTO toDto(PagamentoItem entity);
}
