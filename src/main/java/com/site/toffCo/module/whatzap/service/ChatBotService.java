package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.produto.entity.Produto;
import com.site.toffCo.module.produto.repository.ProdutoRepository;
import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static com.site.toffCo.module.whatzap.dto.ChatState.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private static final int CATALOG_PAGE_SIZE = 3;

    private final WhatsappSessionStore sessionStore;
    private final WhatzapService evolutionApiClient;
    private final ProdutoRepository produtoRepository;

    public void sendResponseClient(String numberClient, String textResponse) {
        SendMessageRequest request = new SendMessageRequest(
                numberClient,
                textResponse,
                2200
        );

        evolutionApiClient.sendMessage(request);
    }

    public String processIncomingMessage(String whatsappId, String messageText) {
        return processIncomingMessage(whatsappId, messageText, true);
    }

    public String simulateIncomingMessage(String whatsappId, String messageText) {
        return processIncomingMessage(whatsappId, messageText, false);
    }

    private String processIncomingMessage(String whatsappId, String messageText, boolean sendToWhatsapp) {
        WhatsappSession session = sessionStore.findByWhatsappId(whatsappId)
                .orElseGet(() -> WhatsappSession.newSession(whatsappId));

        if (session.isHumanAssigned()) {
            sessionStore.save(session);
            return null;
        }

        String responseText = switch (session.getCurrentState()) {
            case MENU_PRINCIPAL -> handleMenuPrincipal(session, messageText);
            case CATALOGO -> handleCatalogo(session, messageText, "PRODUTOS");
            case FILAMENTO -> handleCatalogo(session, messageText, "FILAMENTOS");
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
            session.setCurrentPage(1);
            return buscarProdutosPaginados("FILAMENTOS", session);
        } else if ("3".equals(text)) {
            session.setHumanAssigned(true);
            session.setCurrentState(ChatState.ATENDIMENTO_HUMANO);
            notificarGerente(session.getWhatsappId());
            return BotMessages.HUMAN_ATTENDANCE;
        } else if ("2".equals(text)) {
            session.setCurrentState(CATALOGO);
            session.setCurrentPage(1);
            return buscarProdutosPaginados("PRODUTOS", session);
        }

        return BotMessages.WELCOME_MENU;
    }

    private String handleCatalogo(WhatsappSession session, String text, String categoria) {
        if ("5".equals(text)) {
            session.setCurrentState(MENU_PRINCIPAL);
            session.setCurrentPage(1);
            return BotMessages.BACK_TO_MENU;
        }

        if ("4".equals(text)) {
            session.setCurrentPage(session.getCurrentPage() + 1);
            return buscarProdutosPaginados(categoria, session);
        }

        try {
            int index = Integer.parseInt(text) - 1;
            PageRequest page = PageRequest.of(
                    session.getCurrentPage() - 1,
                    CATALOG_PAGE_SIZE,
                    Sort.by("name")
            );
            Page<Produto> pagina = produtoRepository.findByCategoriaAndAtivoTrue(categoria, page);

            if (index >= 0 && index < pagina.getContent().size()) {
                Produto produto = pagina.getContent().get(index);
                return BotMessages.getProductLink(produto.getName(), session.getCurrentPage());
            }
        } catch (NumberFormatException _) {
            return "Opção invalida. Digite o numero de item ou 5 para voltar.";
        }
        return "Opção invalida. Use 5 para voltar";
    }

    private String buscarProdutosPaginados(String categoria, WhatsappSession session) {
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
            return "Nenhum produto encontrado nesta categoria.";
        }

        return BotMessages.formatDynamicCatalog(
                categoria,
                paginaProduto.getContent(),
                session.getCurrentPage(),
                paginaProduto.hasNext()
        );
    }

    private void notificarGerente(String whatsappId) {
        log.info("Notificando gerente sobre atendimento do WhatsApp {}", whatsappId);
        String messageGerente = "\uD83D\uDD14 *Novo pedido de atendimento!*\\nCliente: " + whatsappId;
        SendMessageRequest request = new SendMessageRequest(
                "5588921498062",
                messageGerente,
                2200
        );
        evolutionApiClient.sendMessage(request);
    }
}
