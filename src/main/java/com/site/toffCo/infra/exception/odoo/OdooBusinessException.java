package com.site.toffCo.infra.exception.odoo;

public class OdooBusinessException extends RuntimeException {
    public OdooBusinessException(String message) {
        super(message);
    }
}
