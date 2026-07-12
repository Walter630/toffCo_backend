package com.site.toffCo.infra.specification;

import com.site.toffCo.module.produto.entity.Produto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class ProductSpec {

    public static Specification<Produto> findByName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Produto> findByDescricao(String description) {
        return (root, query, cb) ->
                description == null ? null : cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }

    public static Specification<Produto> findByCategory(String categoria) {
        return (root, query, cb) ->
                categoria == null ? null : cb.equal(root.get("categoria"), categoria);
    }

    public static Specification<Produto> findByPrice(BigDecimal price) {
        return (root, query, cb) ->
                price == null ? null : cb.equal(root.get("price"), price);
    }

    public static Specification<Produto> findByType(List<String> type) {
        return (root, query, cb) ->
                (type == null || type.isEmpty() || type.contains(null))
                        ? null
                        : root.get("type").in(type);
    }

    public static Specification<Produto> findByTypeIgnoreCase(String type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(cb.lower(root.get("type")), type.toLowerCase());
    }

    public static Specification<Produto> findByStatus(String status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Produto> findByMarca(String marca) {
        return (root, query, cb) ->
                marca == null ? null : cb.equal(root.get("marca"), marca);
    }
}
