package com.site.toffCo.module.produto.infrastructure.persistence.specification;

import com.site.toffCo.module.produto.domain.Produto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class ProductSpec {

    public static Specification<Produto> findByName(String name) {
        return (root, query, cb) -> {
            String normalizedName = normalizeSearchTerm(name);

            if (normalizedName == null) {
                return null;
            }

            return cb.like(
                    cb.function(
                            "unaccent",
                            String.class,
                            cb.lower(root.get("name"))
                    ),
                    "%" + normalizedName + "%"
            );
        };
    }

    public static Specification<Produto> findByDescricao(String description) {
        return (root, query, cb) -> {
            String normalizedDescription = normalizeSearchTerm(description);

            if (normalizedDescription == null) {
                return null;
            }

            return cb.like(
                    cb.function(
                            "unaccent",
                            String.class,
                            cb.lower(root.get("description"))
                    ),
                    "%" + normalizedDescription + "%"
            );
        };
    }

    private static String normalizeSearchTerm(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
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
