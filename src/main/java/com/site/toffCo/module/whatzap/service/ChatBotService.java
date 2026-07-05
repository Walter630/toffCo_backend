package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static com.site.toffCo.module.whatzap.dto.ChatState.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private static final long BOT_ECHO_WINDOW_SECONDS = 5;
    private static final String RESET_COMMAND = "menu";

    private final WhatsappSessionStore sessionStore;
    private final WhatzapService evolutionApiClient;

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
        sessionStore.findByWhatsappId(whatsappId).ifPresent(session -> {
            Instant lastReply = session.getLastBotReplyAt();
            boolean isEchoFromBot = lastReply != null
                    && Duration.between(lastReply, Instant.now()).getSeconds() <= BOT_ECHO_WINDOW_SECONDS;

            if (!isEchoFromBot && !session.isHumanAssigned()) {
                session.setHumanAssigned(true);
                session.setCurrentState(ChatState.ATENDIMENTO_HUMANO);
                sessionStore.save(session);
                log.info("Atendente assumiu manualmente a conversa com {}", whatsappId);
            }
        });
    }

    public void processIncomingMessage(String whatsappId, String messageText, String messageId) {
        processIncomingMessage(whatsappId, messageText, messageId, true);
    }

    public String simulateIncomingMessage(String whatsappId, String messageText, String messageId) {
        return processIncomingMessage(whatsappId, messageText, messageId, false);
    }

    private String processIncomingMessage(String whatsappId, String messageText, String messageId, boolean sendToWhatsapp) {
        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));

        if (messageId != null && messageId.equals(session.getLastMessageId())) {
            return null;
        }

        session.setLastMessageId(messageId);

        if (messageText != null && RESET_COMMAND.equalsIgnoreCase(messageText.trim())) {
            session.setHumanAssigned(false);
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            sessionStore.save(session);

            if (sendToWhatsapp) {
                sendResponseClient(whatsappId, BotMessages.WELCOME_MENU);
            }
            return BotMessages.WELCOME_MENU;
        }

        if (session.isHumanAssigned()) {
            sessionStore.save(session);
            return null;
        }

        String responseText = switch (session.getCurrentState()) {
            case MENU_PRINCIPAL -> handleMenuPrincipal(session, messageText);
            case CATALOGO -> handleCatalogo(session, messageText, "PRODUTOS");
            case FILAMENTO -> handleCatalogo(session, messageText, "FILAMENTOS");
            case MAQUINAS -> handleCatalogo(session, messageText, "MAQUINAS");
            case ACESSORIOS ->  handleCatalogo(session, messageText, "ACESSORIOS");
            case IMPRESSORAS -> handleCatalogo(session, messageText, "IMPRESSORAS");
            case ASSUNTO_ATENDIMENTO -> handleAssuntoAtendimento(session, messageText);
            case DESCRICAO_ATENDIMENTO -> handleDescricaoAtendimento(session, messageText);
            case ATENDIMENTO_HUMANO -> null;
        };

        if (sendToWhatsapp && responseText != null && !responseText.isBlank()) {
            sendResponseClient(whatsappId, responseText);
        }
        sessionStore.save(session);
        return responseText;
    }

    private String handleMenuPrincipal(WhatsappSession session, String text) {
        if ("1".equals(text)) {
            session.setCurrentState(FILAMENTO);
            return BotMessages.getCatalogLink("FILAMENTOS");
        }
        else if ("2".equals(text)) {
            session.setCurrentState(CATALOGO);
            return BotMessages.getCatalogLink("PRODUTOS");
        }
        else if ("3".equals(text)) {
            session.setCurrentState(MAQUINAS);
            return BotMessages.getCatalogLink("MAQUINAS");
        }
        else if ("4".equals(text)) {
            session.setCurrentState(ACESSORIOS);
            return BotMessages.getCatalogLink("ACESSORIOS");
        }
        else if ("5".equals(text)) {
            session.setCurrentState(IMPRESSORAS);
            return BotMessages.getCatalogLink("IMPRESSORAS");
        }
        else if ("6".equals(text)) {
            session.setCurrentState(ASSUNTO_ATENDIMENTO);
            return BotMessages.ATTENDANCE_SUBJECT_MENU;
        }

        return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.WELCOME_MENU;
    }

    private String handleAssuntoAtendimento(WhatsappSession session, String text) {
        if ("0".equals(text)) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            session.setAttendanceSubject(null);
            return BotMessages.BACK_TO_MENU;
        }

        String object = switch (text) {
            case "1" -> "Mentoria";
            case "2" -> "Manutenção em máquina";
            case "3" -> "Compra em atacado acima de 30kg";
            case "4" -> "Dúvida sobre catálogo, produto ou máquina";
            case "5" -> "Outro assunto";
            default -> null;
        };

        if (object == null) {
            return BotMessages.INVALID_OPTION + "\n\n" + BotMessages.ATTENDANCE_SUBJECT_MENU;
        }

        session.setAttendanceSubject(object);
        session.setCurrentState(DESCRICAO_ATENDIMENTO);

        return BotMessages.askProblemDescription(object);
    }

    private String handleDescricaoAtendimento(WhatsappSession session, String text) {
        if (text == null || text.isBlank()) {
            return BotMessages.askProblemDescription(session.getAttendanceSubject());
        }
        session.setHumanAssigned(true);
        session.setCurrentState(ATENDIMENTO_HUMANO);

        notificarGerente(
                session.getWhatsappId(),
                session.getAttendanceSubject(),
                text
        );

        return BotMessages.WAITING_ATTENDANT_WITH_LINK;
    }

    private String handleCatalogo(WhatsappSession session, String text, String catalogo) {
        if ("7".equals(text)) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            return BotMessages.BACK_TO_MENU;
        }
        return "Digite 7 para voltar ao catalogo.";
    }

    /*private String buscarProdutosPaginados(String categoria, WhatsappSession session) {
        PageRequest pageRequest = PageRequest.of(
                session.getCurrentPage() - 1,
                CATALOG_PAGE_SIZE,
                Sort.by("name")
        );
        Page<Produto> paginaProduto = produtoRepository.findByCategoriaAndAtivoTrue(categoria, pageRequest);

        if (paginaProduto.isEmpty()) {
            if (session.getCurrentPage() > 1) {
                session.setCurrentPage(1);
                return buscarProdutosPaginados(categoria, session);
            }
            session.setCurrentPage(1);
            session.setCurrentState(MENU_PRINCIPAL);

            return "Nenhum produto encontrado nesta categoria. \n\n" +
                    BotMessages.formatDynamicCatalog(categoria,
                            paginaProduto.getContent(),
                            session.getCurrentPage(),
                            paginaProduto.hasNext());
        }

        return BotMessages.formatDynamicCatalog(
                categoria,
                paginaProduto.getContent(),
                session.getCurrentPage(),
                paginaProduto.hasNext()
        );
    }*/

    private void notificarGerente(String whatsappId, String subject, String message) {
        log.info("Notificando gerente sobre atendimento do WhatsApp {}", whatsappId);
        String messageGerente = BotMessages.managerNotification(
                whatsappId,
                subject,
                message
        );

        SendMessageRequest request = new SendMessageRequest(
                "553484114981",
                messageGerente,
                2200
        );
        evolutionApiClient.sendMessage(request);
    }
}
