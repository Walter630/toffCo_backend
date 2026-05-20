package com.site.toffCo.infra.exception.user;

public class EmailIsExisting extends RuntimeException {
    public EmailIsExisting(String message) {
        super(message);
    }
}
