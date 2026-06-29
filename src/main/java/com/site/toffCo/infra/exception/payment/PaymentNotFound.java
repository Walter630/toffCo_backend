package com.site.toffCo.infra.exception.payment;

public class PaymentNotFound extends RuntimeException {
    public PaymentNotFound(String message) {
        super(message);
    }
}
