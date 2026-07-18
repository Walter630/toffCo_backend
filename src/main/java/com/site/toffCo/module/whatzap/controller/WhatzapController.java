package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import com.site.toffCo.module.whatzap.service.AttendanceQueueService;
import com.site.toffCo.module.whatzap.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatzapController {

    private final ChatBotService chatBotService;
    private final AttendanceQueueService queueService;

    private static final String ATTENDANT_NUMBER = "553488560330";

    @PostMapping("/receive")
    public ResponseEntity<Void> receiveMessage(@RequestBody WebhookPayload payload) {
        if (payload.data() == null
                || payload.data().key() == null
                || payload.data().key().remoteJid() == null
                || payload.data().message() == null) {
            return ResponseEntity.ok().build();
        }

        String remoteJid = payload.data().key().remoteJid();
        String text = payload.data().message().text();

        // Se for @lid, pega o número real do remoteJidAlt
        if (remoteJid.endsWith("@lid")) {
            remoteJid = payload.data().key().remoteJidAlt();
            if (remoteJid == null || !remoteJid.endsWith("@s.whatsapp.net")) {
                return ResponseEntity.ok().build();
            }
        }

        if (text == null || text.isBlank() || !remoteJid.endsWith("@s.whatsapp.net")) {
            return ResponseEntity.ok().build();
        }

        String number = remoteJid.replace("@s.whatsapp.net", "");
        boolean isFromMe = Boolean.TRUE.equals(payload.data().key().fromMe());

        if (isFromMe) {
            if (number.equals(ATTENDANT_NUMBER) && text.startsWith("/")) {
                String response = queueService.handleAttendantCommand(number, text);
                if (response != null) {
                    chatBotService.sendResponseClient(number, response);
                }
                return ResponseEntity.ok().build();
            }
            chatBotService.handlePossibleHumanIntervention(number);
            return ResponseEntity.ok().build();
        }

        String messageId = payload.data().key().id();
        chatBotService.updateLastMessageIfHumanAssigned(number, text);
        chatBotService.processIncomingMessage(number, text, messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/queue")
    public ResponseEntity<?> getQueue() {
        return ResponseEntity.ok(queueService.getPendingQueue());
    }

    @PostMapping("/queue/{clientNumber}/assign")
    public ResponseEntity<?> assignAttendant(@PathVariable String clientNumber,
                                             @RequestParam String attendantNumber) {
        boolean ok = queueService.assignToAttendant(clientNumber, attendantNumber);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/queue/{clientNumber}/release")
    public ResponseEntity<?> releaseClient(@PathVariable String clientNumber) {
        boolean ok = queueService.releaseSession(clientNumber);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
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