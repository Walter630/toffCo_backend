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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.site.toffCo.module.whatzap.dto.ChatState.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private static final long BOT_ECHO_WINDOW_SECONDS = 5;
    private static final String RESET_COMMAND = "menu";

    private final WhatsappSessionStore sessionStore;
    private final WhatzapService evolutionApiClient;

    /*
     * Número do atendente/gerente que recebe notificações do bot.
     * Configurado em application.yaml: whatsapp.attendant-number
     */
    @Value("${whatsapp.attendant-number:553488560330}")
    private String attendantNumber;

    // ─── API PÚBLICA ──────────────────────────────────────────────

    public void sendResponseClient(String numberClient, String textResponse) {
        SendMessageRequest request = new SendMessageRequest(
                numberClient,
                textResponse,
                2200
        );

        evolutionApiClient.sendMessage(request);

        sessionStore.findByWhatsappId(numberClient).ifPresent(session -> {
            session.setLastBotReplyAt(Instant.now());
            sessionStore.save(session);
        });
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
        processIncomingMessage(whatsappId, messageText, messageId, true);
    }

    public String simulateIncomingMessage(String whatsappId, String messageText, String messageId) {
        return processIncomingMessage(whatsappId, messageText, messageId, false);
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
        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));

        // Deduplicação: ignora mensagem que já foi processada
        if (messageId != null && messageId.equals(session.getLastMessageId())) {
            return null;
        }
        session.setLastMessageId(messageId);

        // Comando de reset: só reseta se NÃO estiver em atendimento humano.
        // Se estiver com humano, "menu" é ignorado pelo bot — o atendente precisa
        // usar /finalizar para devolver o controle.
        if (messageText != null && RESET_COMMAND.equalsIgnoreCase(messageText.trim())) {
            if (session.isHumanAssigned()) {
                // Atendimento humano ativo: bot não interfere, nem com "menu"
                sessionStore.save(session);
                return null;
            }
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            sessionStore.save(session);

            if (sendToWhatsapp) {
                sendResponseClient(whatsappId, BotMessages.WELCOME_MENU);
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
            case DESCRICAO_ATENDIMENTO -> handleDescricaoAtendimento(session, messageText);
            case ATENDIMENTO_HUMANO   -> null;
        };

        if (sendToWhatsapp && responseText != null && !responseText.isBlank()) {
            sendResponseClient(whatsappId, responseText);
        }

        sessionStore.save(session);
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
        /*
         * Switch expression (Java 14+ estável): substitui if/else encadeados.
         * Cada case é uma expressão — sem fall-through acidental, sem return espalhado.
         */
        return switch (text) {
            case "1" -> { session.setCurrentState(FILAMENTO);           yield BotMessages.getCatalogLink("FILAMENTOS"); }
            case "2" -> { session.setCurrentState(CATALOGO);            yield BotMessages.getCatalogLink("PRODUTOS"); }
            case "3" -> { session.setCurrentState(MAQUINAS);            yield BotMessages.getCatalogLink("MAQUINAS"); }
            case "4" -> { session.setCurrentState(ACESSORIOS);          yield BotMessages.getCatalogLink("ACESSORIOS"); }
            case "5" -> { session.setCurrentState(ASSUNTO_ATENDIMENTO); yield BotMessages.ATTENDANCE_SUBJECT_MENU; }
            default  -> BotMessages.INVALID_OPTION + "\n\n" + BotMessages.WELCOME_MENU;
        };
    }

    private String handleAssuntoAtendimento(WhatsappSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            session.setAttendanceSubject(null);
            return BotMessages.BACK_TO_MENU;
        }

        String subject = switch (text) {
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

    private String handleDescricaoAtendimento(WhatsappSession session, String text) {
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
        notificarGerente(session.getWhatsappId(), session.getAttendanceSubject(), text);
        return null;
    }

    private String handleCatalogo(WhatsappSession session, String text, String catalogo) {
        if ("0".equals(text)) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            return BotMessages.BACK_TO_MENU;
        }
        return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.ATTENDANCE_SUBJECT_MENU;
    }

    // ─── NOTIFICAÇÃO ──────────────────────────────────────────────

    private void notificarGerente(String whatsappId, String subject, String message) {
        log.info("Notificando gerente sobre atendimento do WhatsApp {}", whatsappId);

        SendMessageRequest requestCliente = new SendMessageRequest(
                whatsappId,
                BotMessages.WAITING_ATTENDANT_WITH_LINK,
                2200
        );

        SendMessageRequest requestGerente = new SendMessageRequest(
                attendantNumber,
                BotMessages.managerNotification(whatsappId, subject, message),
                2200
        );

        /*
         * Envia as duas mensagens em paralelo via StructuredTaskScope.
         * Tempo total = max(t_cliente, t_gerente) em vez de t_cliente + t_gerente.
         */
        evolutionApiClient.sendMessages(List.of(requestCliente, requestGerente));
    }
}
