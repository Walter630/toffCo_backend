package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.ChatStatus;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.site.toffCo.module.whatzap.dto.ChatState.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private static final long BOT_ECHO_WINDOW_SECONDS = 5;
    private static final String RESET_COMMAND = "menu";

    private final WhatsappSessionStore sessionStore;
    private final WhatzapService evolutionApiClient;
    private final ConcurrentMap<String, Object> processingLocks = new ConcurrentHashMap<>();

    /*
     * Número do atendente/gerente que recebe notificações do bot.
     * Configurado em application.yaml: whatsapp.attendant-number
     */
    @Value("${whatsapp.attendant-number:553488560330}")
    private String attendantNumber;

    // ─── API PÚBLICA ──────────────────────────────────────────────

    public boolean sendResponseClient(String numberClient, String textResponse) {
        try {
            if (sessionStore.isResponseDuplicate(numberClient, textResponse)) {
                log.info("Resposta duplicada bloqueada: cliente={}", numberClient);
                return true;
            }
        } catch (RuntimeException exception) {
            // Se o Redis estiver indisponível, o bot ainda tenta responder.
            log.warn("Idempotência indisponível; enviando resposta normalmente: {}", exception.getMessage());
        }
        evolutionApiClient.sendTyping(numberClient);
        SendMessageRequest request = new SendMessageRequest(
                numberClient,
                textResponse,
                2200
        );

        // Marca antes da chamada externa para fechar a janela de corrida:
        // a Evolution pode emitir o webhook fromMe antes de o POST terminar.
        markLastBotReply(numberClient);
        boolean send = evolutionApiClient.sendMessage(request);

        if (send) {
            // Toda mensagem enviada pelo bot precisa deixar uma marca na sessão.
            // A Evolution também devolve essas mensagens no webhook com fromMe=true;
            // sem essa marca, o próprio bot seria confundido com um atendente humano.
            sessionStore.findByWhatsappId(numberClient).ifPresent(session -> {
                session.setLastBotReplyAt(Instant.now());
                sessionStore.save(session);
            });
        }

        if (!send) {
            try {
                sessionStore.releaseResponseClaim(numberClient, textResponse);
            } catch (RuntimeException exception) {
                log.warn("Não foi possível liberar a chave de idempotência: {}", exception.getMessage());
            }
            log.warn("Falha ao send chat response {}", textResponse != null ? textResponse.length() : 0);
        }

         return send;
    }

    private void markLastBotReply(String numberClient) {
        sessionStore.findByWhatsappId(numberClient).ifPresent(session -> {
            session.setLastBotReplyAt(Instant.now());
            sessionStore.save(session);
        });
    }

    /**
     * Envia uma mensagem do bot e registra o horário
     * usando a mesma instância da sessão que está sendo processada.
     */
    private boolean sendBotMessage(
            WhatsappSession session,
            String text
    ) {
        boolean sent = sendResponseClient(
                session.getWhatsappId(),
                text
        );

        if (!sent) {
            return false;
        }

        session.setLastBotReplyAt(Instant.now());
        sessionStore.save(session);

        return true;
    }

    public void handlePossibleHumanIntervention(String whatsappId) {
        // Busca ou cria a sessão — se não existe, o atendente está abrindo
        // o chat pela primeira vez e o bot nunca deve assumir essa conversa
        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));

        Instant lastReply = session.getLastBotReplyAt();
        boolean isEchoFromBot = lastReply != null
                && Duration.between(lastReply, Instant.now()).getSeconds() <= BOT_ECHO_WINDOW_SECONDS;

        if (!isEchoFromBot && !session.isHumanAssigned()) {
            session.setHumanAssigned(true);
            session.setCurrentState(ChatState.ATENDIMENTO_HUMANO);
            sessionStore.save(session);
            log.info("Atendente assumiu manualmente a conversa com {}", whatsappId);
        }
    }

    public void processIncomingMessage(String whatsappId, String messageText, String messageId) {
        synchronized (processingLocks.computeIfAbsent(whatsappId, ignored -> new Object())) {
            processIncomingMessage(whatsappId, messageText, messageId, true);
        }
    }

    /** Mantém o webhook rápido mesmo quando a Evolution API estiver lenta. */
    @Async("whatsappBotExecutor")
    public void processIncomingMessageAsync(String whatsappId, String messageText, String messageId) {
        try {
            processIncomingMessage(whatsappId, messageText, messageId);
        } catch (Exception exception) {
            log.error("Falha ao processar mensagem WhatsApp do número {}: {}", whatsappId,
                    exception.getMessage(), exception);
            sendResponseClient(whatsappId, BotMessages.SYSTEM_FAILURE);
        }
    }

    public String simulateIncomingMessage(String whatsappId, String messageText, String messageId) {
        synchronized (processingLocks.computeIfAbsent(whatsappId, ignored -> new Object())) {
            return processIncomingMessage(whatsappId, messageText, messageId, false);
        }
    }

    public void updateLastMessageIfHumanAssigned(String whatsappId, String messageText) {
        sessionStore.findByWhatsappId(whatsappId).ifPresent(session -> {
            if (session.isHumanAssigned()) {
                session.setLastMessage(messageText);
                sessionStore.save(session);
            }
        });
    }

    /** Retorna true se a sessão do número está em atendimento humano ativo. */
    public boolean isHumanAssigned(String whatsappId) {
        return sessionStore.findByWhatsappId(whatsappId)
                .map(WhatsappSession::isHumanAssigned)
                .orElse(false);
    }

    /**
     * Reseta a sessão para MENU_PRINCIPAL sem enviar mensagem.
     * Usado quando o cliente manda mídia — a sessão volta pro início
     * para o próximo texto dele ser interpretado corretamente no menu.
     */
    public void resetToMenu(String whatsappId) {
        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));
        session.setCurrentState(ChatState.MENU_PRINCIPAL);
        session.setCurrentPage(1);
        sessionStore.save(session);
    }

    // ─── PROCESSAMENTO INTERNO ────────────────────────────────────

    private String processIncomingMessage(
            String whatsappId,
            String messageText,
            String messageId,
            boolean sendToWhatsapp
    ) {
        if (!sessionStore.markMessageAsProcessed(messageId, whatsappId, messageText)) {
            log.info(
                    "Mensagem duplicada ignorada: messageId={}, cliente={}",
                    messageId,
                    whatsappId
            );

            return null;
        }

        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));

        session.setLastMessageId(messageId);

        // Comando de reset: só reseta se NÃO estiver em atendimento humano.
        // Se estiver com humano, "menu" é ignorado pelo bot — o atendente precisa
        // usar /finalizar para devolver o controle.
        if (messageText != null
                && RESET_COMMAND.equalsIgnoreCase(messageText.trim())) {

            if (session.isHumanAssigned()) {
                sessionStore.save(session);
                return null;
            }

            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);

            // Salva o estado antes do envio.
            sessionStore.save(session);

            if (sendToWhatsapp) {
                sendBotMessage(
                        session,
                        BotMessages.WELCOME_MENU
                );
            }

            return BotMessages.WELCOME_MENU;
        }

        // Se está em atendimento humano, bot silencia completamente
        if (session.isHumanAssigned()) {
            sessionStore.save(session);
            return null;
        }

        /*
         * Roteamento por estado atual da sessão.
         * Switch expression exaustivo — o compilador garante que todos os
         * casos do enum ChatState estão cobertos.
         */
        String responseText = switch (session.getCurrentState()) {
            case NOVO                 -> handleNovo(session);
            case MENU_PRINCIPAL       -> handleMenuPrincipal(session, messageText);
            case CATALOGO             -> handleCatalogo(session, messageText, "PRODUTOS");
            case FILAMENTO            -> handleCatalogo(session, messageText, "FILAMENTOS");
            case MAQUINAS             -> handleCatalogo(session, messageText, "MAQUINAS");
            case ACESSORIOS           -> handleCatalogo(session, messageText, "ACESSORIOS");
            case ASSUNTO_ATENDIMENTO  -> handleAssuntoAtendimento(session, messageText);
            case DESCRICAO_ATENDIMENTO -> handleDescricaoAtendimento(
                    session,
                    messageText,
                    sendToWhatsapp
            );
            case ATENDIMENTO_HUMANO   -> null;
        };

        sessionStore.save(session);

        if (sendToWhatsapp && responseText != null && !responseText.isBlank()) {
            boolean sent = sendResponseClient(whatsappId, responseText);

            // O envio ao cliente vem primeiro. O n8n só observa o resultado;
            // ele nunca pode atrasar ou impedir a resposta do WhatsApp.
            evolutionApiClient.notifyBotResponseReview(
                    messageId,
                    whatsappId,
                    messageText,
                    responseText,
                    session.getCurrentState().name()
            );

            if (!sent) {
                sendResponseClient(whatsappId, BotMessages.SYSTEM_FAILURE);
            }
        }

        return responseText;
    }

    // ─── HANDLERS DE ESTADO ───────────────────────────────────────

    /**
     * Estado inicial da sessão: qualquer mensagem do usuário (saudação, texto livre, etc.)
     * exibe o menu principal pela primeira vez, sem tratar como opção inválida.
     */
    private String handleNovo(WhatsappSession session) {
        session.setCurrentState(MENU_PRINCIPAL);
        return BotMessages.WELCOME_MENU;
    }

    private String handleMenuPrincipal(WhatsappSession session, String text) {
        if (text == null || text.isBlank()) {
            return BotMessages.WELCOME_MENU;
        }
        String normalizedText = text.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedText.matches("oi|olá|ola|bom dia|boa tarde|boa noite")) {
            return BotMessages.WELCOME_MENU;
        }
        /*
         * Switch expression (Java 14+ estável): substitui if/else encadeados.
         * Cada case é uma expressão — sem fall-through acidental, sem return espalhado.
         */
        //trim remove os espaços invalidos do texto
        return switch (normalizedText) {
            case "1" -> {
                session.setCurrentState(CATALOGO);
                session.setCurrentPage(1);
                yield BotMessages.getCatalogLink("PRODUTOS");
            }
            case "2" -> {
                session.setAttendanceSubject("Manutenção de impressoras 3D");
                session.setCurrentState(DESCRICAO_ATENDIMENTO);
                yield BotMessages.askProblemDescription(session.getAttendanceSubject());
            }
            case "3" -> {
                session.setAttendanceSubject("Consultoria em impressão 3D");
                session.setCurrentState(DESCRICAO_ATENDIMENTO);
                yield BotMessages.askProblemDescription(session.getAttendanceSubject());
            }
            case "4" -> { session.setCurrentState(ASSUNTO_ATENDIMENTO); yield BotMessages.ATTENDANCE_SUBJECT_MENU; }
            // Texto livre no menu nÃ£o deve parecer um erro para o cliente.
            // Mostra o menu novamente e deixa a conversa seguir normalmente.
              default  -> BotMessages.WELCOME_MENU;
        };
    }

    private String handleAssuntoAtendimento(WhatsappSession session, String text) {
        if (text == null || text.isBlank()) {
            return BotMessages.ATTENDANCE_SUBJECT_MENU;
        }
        if ("0".equals(text.trim())) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            session.setAttendanceSubject(null);
            return BotMessages.BACK_TO_MENU;
        }

        String subject = switch (text.trim()) {
            case "1" -> "Mentoria";
            case "2" -> "Manutenção em máquina";
            case "3" -> "Compra em atacado acima de 30kg";
            case "4" -> "Dúvida sobre catálogo, produto ou máquina";
            case "5" -> "Outro assunto";
            default  -> null;
        };

        if (subject == null) {
            return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.ATTENDANCE_SUBJECT_MENU;
        }

        session.setAttendanceSubject(subject);
        session.setCurrentState(DESCRICAO_ATENDIMENTO);
        return BotMessages.askProblemDescription(subject);
    }

    private String handleDescricaoAtendimento(
            WhatsappSession session,
            String text,
            boolean sendToWhatsapp
    ) {
        if (text == null || text.isBlank()) {
            return BotMessages.askProblemDescription(session.getAttendanceSubject());
        }

        session.setHumanAssigned(true);
        session.setCurrentState(ATENDIMENTO_HUMANO);
        session.setStatus(ChatStatus.PENDING);
        session.setHumanAssingnedAt(Instant.now());
        session.setLastMessage(text);
        session.setResolvedBy("HUMANO");

        /*
         * notificarGerente envia WAITING_ATTENDANT_WITH_LINK ao cliente
         * e a notificação ao gerente em paralelo via StructuredTaskScope.
         * Retornamos null para não duplicar o envio em sendResponseClient.
         */
        /*
         * Salva antes dos envios.
         * Se outro webhook chegar enquanto as mensagens estão sendo enviadas,
         * o bot já saberá que a conversa está em atendimento humano.
        */
        sessionStore.save(session);

        if (sendToWhatsapp) {
            notificarGerente(session, text);
        } else {
            publishHumanAttendanceRequested(session, text, false, false);
        }

        return null;
    }

    private String handleCatalogo(WhatsappSession session, String text, String catalogo) {
        if (text == null || text.isBlank()) {
            return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.getCatalogLink(catalogo);
        }
        if ("0".equals(text.trim())) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            return BotMessages.BACK_TO_MENU;
        }
        return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.getCatalogLink(catalogo);
    }

    // ─── NOTIFICAÇÃO ──────────────────────────────────────────────

    private void notificarGerente(
            WhatsappSession session,
            String message
    ) {
        String whatsappId =
                session.getWhatsappId();

        String subject =
                session.getAttendanceSubject();

        log.info(
                "Cliente {} solicitou atendimento humano. Assunto: {}",
                whatsappId,
                subject
        );

        /*
         * Confirma para o cliente.
         */
        boolean clientNotified = sendBotMessage(
                session,
                BotMessages.WAITING_ATTENDANT_WITH_LINK
        );

        if (!clientNotified) {
            /*
             * Mesmo que a confirmação ao cliente falhe,
             * ainda tentaremos avisar o gerente.
             *
             * Caso contrário, um possível cliente interessado
             * seria perdido por causa de uma falha temporária.
             */
            log.warn(
                    "Cliente {} não recebeu a confirmação",
                    whatsappId
            );
        }

        /*
         * Impede alertas duplicados ao gerente.
         */
        if (!sessionStore.markManagerNotification(whatsappId)) {
            log.info(
                    "Gerente já foi notificado recentemente sobre {}",
                    whatsappId
            );

            return;
        }

        SendMessageRequest managerRequest =
                new SendMessageRequest(
                        attendantNumber,
                        BotMessages.managerNotification(
                                whatsappId,
                                subject,
                                message
                        ),
                        2200
                );

        boolean managerNotified =
                evolutionApiClient.sendMessage(managerRequest);

        publishHumanAttendanceRequested(session, message, managerNotified, true);
    }

    private void publishHumanAttendanceRequested(
            WhatsappSession session,
            String message,
            boolean managerNotified,
            boolean notificationAttempted
    ) {
        String whatsappId = session.getWhatsappId();
        String subject = session.getAttendanceSubject();

        evolutionApiClient.publishAutomationEvent(
                "HUMAN_ATTENDANCE_REQUESTED",
                session.getLastMessageId(),
                Map.of(
                        "number", whatsappId,
                        "subject", subject == null ? "" : subject,
                        "description", message == null ? "" : message,
                        "managerNotified", managerNotified
                )
        );

        if (notificationAttempted && !managerNotified) {
            /*
             * O envio falhou.
             *
             * Remove a trava para permitir uma nova tentativa futura.
             */
            sessionStore.clearManagerNotification(whatsappId);

            log.warn(
                    "Falha ao notificar gerente sobre o cliente {}",
                    whatsappId
            );
        }
    }
}
