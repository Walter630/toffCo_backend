package com.site.toffCo.module.produto.infrastructure.persistence.specification;

import com.site.toffCo.module.produto.domain.Produto;
import com.site.toffCo.module.produto.domain.ProductStatus;
import com.site.toffCo.module.produto.domain.ProductType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

public final class ProductSpec {

    private ProductSpec() {
    }

    public static Specification<Produto> findByName(String name) {
        return (root, query, cb) -> {
            String normalizedName = normalizeSearchTerm(name);
            if (normalizedName == null) {
                return null;
            }
            return cb.like(
                    cb.function("unaccent", String.class, cb.lower(root.get("name"))),
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
                    cb.function("unaccent", String.class, cb.lower(root.get("description"))),
                    "%" + normalizedDescription + "%"
            );
        };
    }

    public static Specification<Produto> findByPrice(BigDecimal price) {
        return (root, query, cb) -> price == null ? null : cb.equal(root.get("price"), price);
    }

    public static Specification<Produto> findByCategoria(String categoria) {
        return (root, query, cb) -> isBlank(categoria) ? null : cb.equal(root.get("categoria"), categoria);
    }

    public static Specification<Produto> findByType(String type) {
        return (root, query, cb) -> {
            if (isBlank(type)) {
                return null;
            }
            ProductType parsed = parseEnum(ProductType.class, type, "type");
            return cb.equal(root.get("type"), parsed);
        };
    }

    public static Specification<Produto> findByStatus(String status) {
        return (root, query, cb) -> {
            if (isBlank(status)) {
                return null;
            }
            ProductStatus parsed = parseEnum(ProductStatus.class, status, "status");
            return cb.equal(root.get("status"), parsed);
        };
    }

    public static Specification<Produto> findByMarca(String marca) {
        return (root, query, cb) -> isBlank(marca) ? null : cb.equal(root.get("marca"), marca);
    }

    /** Código de barras é String e o match é sempre exato, antes da paginação. */
    public static Specification<Produto> findByCodigoBarras(String codigoBarras) {
        return (root, query, cb) -> {
            String normalized = Produto.normalizarCodigoBarras(codigoBarras);
            return normalized == null ? null : cb.equal(root.get("codigoBarras"), normalized);
        };
    }

    private static String normalizeSearchTerm(String value) {
        if (isBlank(value)) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <E extends Enum<E>> E parseEnum(
            Class<E> enumType,
            String value,
            String field
    ) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Valor inválido para " + field + ": " + value);
        }
    }
}
