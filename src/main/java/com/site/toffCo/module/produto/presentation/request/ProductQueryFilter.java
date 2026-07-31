package com.site.toffCo.module.produto.presentation.request;

import com.site.toffCo.module.produto.domain.Produto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;

import static com.site.toffCo.module.produto.infrastructure.persistence.specification.ProductSpec.*;

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

    public Specification<Produto> buildSpecification() {
        return Specification
                .where(findByName(name))
                .and(findByDescricao(description))
                .and(findByPrice(price))
                .and(findByType(Collections.singletonList(type)))
                .and(findByCategory(categoria))
                .and(findByStatus(status))
                .and(findByMarca(marca));
    }
}
