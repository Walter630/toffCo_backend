package com.site.toffCo.module.pagamentoitem.mapper;

import com.site.toffCo.module.pagamentoitem.entity.PagamentoItem;
import com.site.toffCo.module.pagamentoitem.dto.PagamentoItemResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class PagamentoItemMapper {
    public abstract PagamentoItemResponseDTO toDto(PagamentoItem entity);
}
