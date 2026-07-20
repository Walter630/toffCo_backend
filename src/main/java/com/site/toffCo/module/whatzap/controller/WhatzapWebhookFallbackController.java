package com.site.toffCo.module.whatzap.controller;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatzapWebhookFallbackController {

    private final WhatsAppController mainController;

    @PostMapping("/receive/**")
    public ResponseEntity<Void> receiveMessage(@RequestBody JsonNode request) {
        return mainController.receiveMessage(request);
    }
}