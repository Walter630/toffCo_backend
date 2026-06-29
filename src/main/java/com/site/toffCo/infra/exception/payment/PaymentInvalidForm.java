package com.site.toffCo.infra.exception.payment;

public class PaymentInvalidForm extends RuntimeException {
    public PaymentInvalidForm(String message) {
        super(message);
    }
}
