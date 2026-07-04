package com.site.toffCo.infra.exception.carrinho;

public class CarIsEmpty extends RuntimeException {
    public CarIsEmpty(String message) {
        super(message);
    }
}
