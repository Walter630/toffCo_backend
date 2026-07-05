package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import com.site.toffCo.module.whatzap.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatzapController {

    private final ChatBotService chatBotService;

    @PostMapping("/receive")
    public ResponseEntity<Void> receiveMessage(@RequestBody WebhookPayload payload) {
        if (payload.data() == null
                || payload.data().key() == null
                || Boolean.TRUE.equals(payload.data().key().fromMe())
                || payload.data().key().remoteJid() == null
                || payload.data().message() == null) {
            return ResponseEntity.ok().build();
        }

        String remoteJid = payload.data().key().remoteJid();
        String text = payload.data().message().text();

        if (text == null || text.isBlank() || !remoteJid.endsWith("@s.whatsapp.net")) {
            return ResponseEntity.ok().build();
        }

        String number = remoteJid.replace("@s.whatsapp.net", "");
        if (Boolean.TRUE.equals(payload.data().key().fromMe())) {
            chatBotService.handlePossibleHumanIntervention(number);
            return ResponseEntity.ok().build();
        }

        String messageId = payload.data().key().id();
        chatBotService.processIncomingMessage(number, text, messageId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateMessage(@RequestBody SimulationRequest request) {
        if (request.number() == null || request.number().isBlank()
                || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Os campos 'number' e 'message' são obrigatórios."
            ));
        }

        String response = chatBotService.simulateIncomingMessage(request.number(), request.message().trim(), request.messageId);

        if (response == null) {
            return ResponseEntity.ok(Map.of(
                    "number", request.number(),
                    "response", "Conversa encaminhada para atendimento humano aguarde um momento..."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "number", request.number(),
                "response", response
        ));
    }

    public record SimulationRequest(String number, String message, String messageId) {}
}
