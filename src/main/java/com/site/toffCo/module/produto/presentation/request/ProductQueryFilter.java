package com.site.toffCo.module.produto.presentation.request;

import com.site.toffCo.module.produto.domain.Produto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByCategoria;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByCodigoBarras;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByDescricao;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByMarca;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByName;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByPrice;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByStatus;
import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.findByType;

@Getter
@Setter
public class ProductQueryFilter {
    private String name;
    private String description;
    private BigDecimal price;
    private String categoria;
    private String type;
    private String status;
    private String marca;
    private String codigoBarras;

    public Specification<Produto> buildSpecification() {
        return Specification
                .where(findByName(name))
                .and(findByDescricao(description))
                .and(findByPrice(price))
                .and(findByType(type))
                .and(findByCategoria(categoria))
                .and(findByStatus(status))
                .and(findByMarca(marca))
                .and(findByCodigoBarras(codigoBarras));
    }
}
