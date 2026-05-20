package com.site.toffCo.infra.exception.product;

public class CategoryNotExisting extends RuntimeException {
    public CategoryNotExisting(String message) {
        super(message);
    }
}
