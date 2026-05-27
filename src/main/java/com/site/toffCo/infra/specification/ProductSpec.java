package com.site.toffCo.infra.specification;

import com.site.toffCo.module.produto.entity.Produto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpec {

    public static Specification<Produto> findByName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Produto> findByDescricao(String descricao) {
        return (root, query, cb) ->
                descricao == null ? null : cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%");
    }

    public static Specification<Produto> findByCategory(String categoria) {
        return (root, query, cb) ->
                categoria == null ? null : cb.equal(root.get("categoria"), categoria);
    }

    public static Specification<Produto> findByPrice(BigDecimal price) {
        return (root, query, cb) ->
                price == null ? null : cb.equal(root.get("price"), price);
    }
}
