package com.site.toffCo.infra.exception;

import java.util.Map;

public record ApiErrorResponse(
        String message,
        String code,
        Map<String, Object> details
) {
    public ApiErrorResponse(String message, String code) {
        this(message, code, Map.of());
    }
}
