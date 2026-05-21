package com.site.toffCo.infra.exception.carrinho;

public class CarNotFound extends RuntimeException {
    public CarNotFound(String message) {
        super(message);
    }
}
