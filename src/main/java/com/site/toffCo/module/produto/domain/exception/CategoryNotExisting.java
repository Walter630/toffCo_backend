package com.site.toffCo.module.produto.domain.exception;

public class CategoryNotExisting extends RuntimeException {
    public CategoryNotExisting(String message) {
        super(message);
    }
}
