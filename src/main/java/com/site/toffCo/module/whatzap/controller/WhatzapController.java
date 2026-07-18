package com.site.toffCo.module.whatzap.controller;

import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import com.site.toffCo.module.whatzap.service.AttendanceQueueService;
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
    private final AttendanceQueueService queueService;

    // NÚMERO DO ATENDENTE/GERENTE (pode vir de config/env)
    private static final String ATTENDANT_NUMBER = "553484114981";

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

        if (text == null || text.isBlank() || !remoteJid.endsWith("@s.whatsapp.net")) {
            return ResponseEntity.ok().build();
        }

        String number = remoteJid.replace("@s.whatsapp.net", "");
        boolean isFromMe = Boolean.TRUE.equals(payload.data().key().fromMe());

        // ─── DETECÇÃO: MENSAGEM DO ATENDENTE (fromMe) ─────────────
        if (isFromMe) {
            // Se o atendente mandou um comando tipo /pendentes, /finalizar...
            if (number.equals(ATTENDANT_NUMBER) && text.startsWith("/")) {
                String response = queueService.handleAttendantCommand(number, text);
                if (response != null) {
                    // Responde no próprio chat do atendente (eco do comando)
                    chatBotService.sendResponseClient(number, response);
                }
                return ResponseEntity.ok().build();
            }

            // Se não é comando, pode ser intervenção humana normal
            chatBotService.handlePossibleHumanIntervention(number);
            return ResponseEntity.ok().build();
        }

        // ─── MENSAGEM DO CLIENTE ───────────────────────────────────
        String messageId = payload.data().key().id();

        // Se cliente está em atendimento humano, atualiza a última msg no Redis
        // (pro preview do dashboard e dos comandos /pendentes)
        chatBotService.updateLastMessageIfHumanAssigned(number, text);

        chatBotService.processIncomingMessage(number, text, messageId);
        return ResponseEntity.ok().build();
    }

    // ─── ROTAS DO DASHBOARD ──────────────────────────────────────

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

    // ... simulate permanece igual

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
