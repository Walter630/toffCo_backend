package com.site.toffCo.module.whatzap.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.site.toffCo.module.whatzap.dto.WebhookPayload;
import com.site.toffCo.module.whatzap.service.AttendanceQueueService;
import com.site.toffCo.module.whatzap.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    /*
     * Número do atendente injetado via @Value para não ficar hardcoded.
     * Configure em application.yaml:
     *   whatsapp:
     *     attendant-number: ${WHATSAPP_ATTENDANT_NUMBER:553488560330}
     */
    @Value("${whatsapp.attendant-number:553488560330}")
    private String attendantNumber;

    private static final String EVENT_MESSAGES_UPSERT = "messages.upsert";

    private final ChatBotService chatBotService;
    private final AttendanceQueueService queueService;

    /*
     * ObjectMapper é o bean padrão do Jackson 3 registrado pelo Spring Boot 4.
     * Não precisamos de JsonMapper explícito — ObjectMapper já é a API principal.
     */
    private final ObjectMapper objectMapper;

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
            String text = data.message().text();

            if (remoteJid == null
                    || remoteJid.isBlank()
                    || text == null
                    || text.isBlank()) {

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
                remoteJid = key.remoteJidAlt();

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

            text = text.trim();

            boolean fromMe = Boolean.TRUE.equals(key.fromMe());

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
                            attendantNumber,
                            text
                    );

                    if (response != null) {
                        // Responde no chat do próprio gerente, não no chat do cliente
                        chatBotService.sendResponseClient(attendantNumber, response);
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

            chatBotService.processIncomingMessage(
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
