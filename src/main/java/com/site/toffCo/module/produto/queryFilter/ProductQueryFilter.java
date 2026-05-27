package com.site.toffCo.module.produto.queryFilter;

import static com.site.toffCo.infra.specification.ProductSpec.*;
import com.site.toffCo.module.produto.entity.Produto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductQueryFilter {
    private String name;
    private String description;
    private BigDecimal price;
    private String categoria;

    public Specification<Produto> buildSpecification() {
        return Specification
                .where(findByName(name))
                .and(findByDescricao(description))
                .and(findByPrice(price))
                .and(findByCategory(categoria));
    }
}
