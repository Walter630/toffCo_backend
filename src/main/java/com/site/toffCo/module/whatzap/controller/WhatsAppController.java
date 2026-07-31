package com.site.toffCo.module.whatzap.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import com.site.toffCo.module.whatzap.service.AttendanceQueueService;
import com.site.toffCo.module.whatzap.service.BotMessages;
import com.site.toffCo.module.whatzap.service.ChatBotService;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import com.site.toffCo.module.whatzap.monitoring.WhatsappCircuitBreaker;
import com.site.toffCo.module.whatzap.monitoring.WhatsappMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private static final String EVENT_MESSAGES_UPSERT = "messages.upsert";

    private final ChatBotService chatBotService;
    private final AttendanceQueueService queueService;
    private final WhatsappSessionStore sessionStore;
    private final WhatsappProperties whatsappProperties;
    private final ObjectMapper objectMapper;
    private final WhatsappCircuitBreaker circuitBreaker;
    private final WhatsappMonitoringService monitoring;

    // Número do atendente vem do properties type-safe
    private String attendantNumber() {
        return whatsappProperties.attendantNumber();
    }

    @PostMapping("/receive")
    public ResponseEntity<Void> receiveMessage(@RequestBody JsonNode payload) {
        try {
            String event = payload.path("event").asText();

            /*
             * A Evolution também envia eventos como:
             * chats.update, contacts.update e presence.update.
             *
             * Este controller processa somente mensagens.
             */
            if (!EVENT_MESSAGES_UPSERT.equalsIgnoreCase(event)) {
                log.debug("Evento ignorado: {}", event);
                return ResponseEntity.ok().build();
            }

            JsonNode dataNode = payload.path("data");

            /*
             * Evita o erro:
             * Cannot deserialize WebhookData from Array value.
             */
            if (!dataNode.isObject()) {
                log.warn(
                        "Evento messages.upsert com data inválida. Tipo recebido: {}",
                        dataNode.getNodeType()
                );
                return ResponseEntity.ok().build();
            }

            WebhookPayload.WebhookData data = objectMapper.treeToValue(
                    dataNode,
                    WebhookPayload.WebhookData.class
            );

            if (data == null
                    || data.key() == null
                    || data.message() == null) {

                log.debug("Webhook sem key ou message. Ignorando.");
                return ResponseEntity.ok().build();
            }

            WebhookPayload.WebhookKey key = data.key();

            String remoteJid = key.remoteJid();
            String text      = data.message().text();

            if (remoteJid == null || remoteJid.isBlank()) {
                return ResponseEntity.ok().build();
            }

            /*
             * O bot não processa nenhuma mensagem de grupo.
             */
            if (remoteJid.endsWith("@g.us")) {
                log.debug("Mensagem de grupo ignorada: {}", remoteJid);
                return ResponseEntity.ok().build();
            }

            /*
             * Algumas mensagens privadas podem chegar utilizando @lid.
             * Nesse caso, tentamos usar o remoteJidAlt com o telefone real.
             */
            if (remoteJid.endsWith("@lid")) {
                // Em versões recentes da Evolution/Baileys, o telefone real
                // pode chegar em senderPn quando remoteJidAlt não existe.
                remoteJid = firstNonBlank(key.remoteJidAlt(), key.senderPn());

                if (remoteJid != null && !remoteJid.contains("@")) {
                    remoteJid = remoteJid + "@s.whatsapp.net";
                }

                if (remoteJid == null
                        || !remoteJid.endsWith("@s.whatsapp.net")) {

                    log.warn(
                            "Mensagem LID sem remoteJidAlt válido. messageId={}",
                            key.id()
                    );

                    return ResponseEntity.ok().build();
                }
            }

            /*
             * Aceita somente conversas privadas.
             */
            if (!remoteJid.endsWith("@s.whatsapp.net")) {
                log.debug("JID não privado ignorado: {}", remoteJid);
                return ResponseEntity.ok().build();
            }

            String number = extractNumberFromJid(remoteJid);

            if (number == null) {
                return ResponseEntity.ok().build();
            }

            // Bloqueado estaticamente (application.yaml) ou dinamicamente (Redis)?
            boolean staticallyBlocked =
                    whatsappProperties.isStaticallyBlocked(number);

            boolean dynamicallyBlocked =
                    sessionStore.isBlocked(number);

            if (staticallyBlocked || dynamicallyBlocked) {
                log.debug(
                        "Número bloqueado ignorado: number={}, static={}, redis={}",
                        number,
                        staticallyBlocked,
                        dynamicallyBlocked
                );

                return ResponseEntity.ok().build();
            }

            /*
             * Mensagem de mídia (áudio, imagem, vídeo, documento, sticker).
             * O bot não consegue processar — avisa o cliente se não for fromMe,
             * e só se o cliente não estiver em atendimento humano (o humano
             * pode receber qualquer mídia normalmente).
             */
            boolean fromMe = Boolean.TRUE.equals(key.fromMe());

            if (!fromMe && data.message().isMedia()) {
                boolean humanActive = chatBotService.isHumanAssigned(number);
                if (!humanActive) {
                    String mediaName = data.message().mediaType().friendlyName();
                    log.debug("Mídia recebida ({}), enviando aviso para {}", mediaName, number);
                    // Reseta pro menu principal para o cliente poder escolher
                    // normalmente após receber a resposta — sem risco de bug de estado
                    chatBotService.resetToMenu(number);
                    chatBotService.sendResponseClient(number, BotMessages.unsupportedMediaMessage(mediaName));
                }
                // Se humano está ativo, silencia — o atendente vê a mídia diretamente no WhatsApp
                return ResponseEntity.ok().build();
            }

            // Texto nulo ou vazio (sem ser mídia) — descarta silenciosamente
            if (text == null || text.isBlank()) {
                return ResponseEntity.ok().build();
            }

            text = text.trim();

            log.info(
                    "Mensagem recebida: number={}, fromMe={}, messageId={}",
                    number,
                    fromMe,
                    key.id()
            );

            /*
             * Mensagem enviada pelo próprio WhatsApp conectado (fromMe=true).
             */
            if (fromMe) {
                /*
                 * Comando do gerente — pode vir de qualquer chat onde ele digitar "/".
                 * Ex: gerente abre o chat com o cliente e digita "/info" ou "/finalizar".
                 *
                 * Respondemos de volta para o gerente (attendantNumber),
                 * não para o cliente, para não vazar o comando no chat do cliente.
                 */
                if (text.startsWith("/")) {
                    String response = queueService.handleAttendantCommand(
                            attendantNumber(),
                            text
                    );

                    if (response != null) {
                        chatBotService.sendResponseClient(attendantNumber(), response);
                    }

                    return ResponseEntity.ok().build();
                }

                /*
                 * Atendente mandou mensagem normal (não comando) diretamente ao cliente.
                 * Bot reconhece a intervenção humana e para de responder nessa conversa.
                 */
                chatBotService.handlePossibleHumanIntervention(number);

                return ResponseEntity.ok().build();
            }

            /*
             * Mensagem recebida de um cliente.
             */
            String messageId = key.id();

            chatBotService.updateLastMessageIfHumanAssigned(
                    number,
                    text
            );

            chatBotService.processIncomingMessageAsync(
                    number,
                    text,
                    messageId
            );

            return ResponseEntity.ok().build();

        } catch (Exception exception) {
            /*
             * Retornamos 200 mesmo quando ocorre um erro interno,
             * para impedir repetição contínua do webhook pela Evolution.
             */
            log.error(
                    "Erro processando webhook da Evolution. Payload={}",
                    payload,
                    exception
            );

            return ResponseEntity.ok().build();
        }
    }

    private String extractNumberFromJid(String jid) {
        if (jid == null || jid.isBlank()) {
            return null;
        }

        if (!jid.endsWith("@s.whatsapp.net")) {
            return null;
        }

        String number = jid
                .replace("@s.whatsapp.net", "")
                .replaceAll("\\D", "");

        return number.isBlank() ? null : number;
    }

    @GetMapping("/queue")
    public ResponseEntity<?> getQueue() {
        return ResponseEntity.ok(queueService.getPendingQueue());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        WhatsappCircuitBreaker.Snapshot circuit = circuitBreaker.snapshot();
        WhatsappMonitoringService.Snapshot metrics = monitoring.snapshot();
        return ResponseEntity.ok(Map.of(
                "status", circuit.state() == WhatsappCircuitBreaker.State.OPEN ? "DEGRADED" : "UP",
                "circuit", Map.of(
                        "state", circuit.state().name(),
                        "consecutiveFailures", circuit.consecutiveFailures(),
                        "openedAt", circuit.openedAt() == null ? "" : circuit.openedAt().toString()
                ),
                "metrics", Map.of(
                        "attempts", metrics.attempts(),
                        "successes", metrics.successes(),
                        "failures", metrics.failures(),
                        "circuitBlocked", metrics.circuitBlocked(),
                        "averageLatencyMs", metrics.averageLatencyMs()
                )
        ));
    }

    // ─── ENDPOINTS DE BLOCKLIST ───────────────────────────────────

    @PostMapping("/blocklist/{number}")
    public ResponseEntity<?> blockNumber(@PathVariable String number) {
        sessionStore.blockNumber(number.replaceAll("\\D", ""));
        return ResponseEntity.ok(Map.of("blocked", number));
    }

    @DeleteMapping("/blocklist/{number}")
    public ResponseEntity<?> unblockNumber(@PathVariable String number) {
        sessionStore.unblockNumber(number.replaceAll("\\D", ""));
        return ResponseEntity.ok(Map.of("unblocked", number));
    }

    @GetMapping("/blocklist")
    public ResponseEntity<?> getBlocklist() {
        return ResponseEntity.ok(sessionStore.getBlocklist());
    }

    @PostMapping("/queue/{clientNumber}/assign")
    public ResponseEntity<?> assignAttendant(
            @PathVariable String clientNumber,
            @RequestParam String attendantNumber
    ) {
        boolean assigned = queueService.assignToAttendant(
                clientNumber,
                attendantNumber
        );

        return assigned
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }

    @PostMapping("/queue/{clientNumber}/release")
    public ResponseEntity<?> releaseClient(
            @PathVariable String clientNumber
    ) {
        boolean released = queueService.releaseSession(clientNumber);

        return released
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateMessage(
            @RequestBody SimulationRequest request
    ) {
        if (!whatsappProperties.simulationEnabled()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Simulação desativada neste ambiente."
            ));
        }

        if (request.number() == null
                || request.number().isBlank()
                || request.message() == null
                || request.message().isBlank()) {

            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "Os campos 'number' e 'message' são obrigatórios."
            ));
        }

        String response = chatBotService.simulateIncomingMessage(
                request.number(),
                request.message().trim(),
                request.messageId()
        );

        return ResponseEntity.ok(Map.of(
                "number",
                request.number(),
                "response",
                Objects.requireNonNullElse(response, "Conversa encaminhada para atendimento humano. Aguarde um momento...")
        ));

    }

    public record SimulationRequest(
            String number,
            String message,
            String messageId
    ) {
    }
}
