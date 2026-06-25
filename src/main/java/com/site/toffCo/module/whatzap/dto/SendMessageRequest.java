package com.site.toffCo.module.whatzap.dto;

public record SendMessageRequest(
        String number,
        String text,
        int delay
) {}
