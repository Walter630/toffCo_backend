package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatzapWebhookFallbackController {

    private final WhatzapController mainController;

    @PostMapping("/receive/**")
    public ResponseEntity<Void> receiveMessage(@RequestBody WebhookPayload payload, HttpServletRequest request) {
        return mainController.receiveMessage(payload, request);
    }
}