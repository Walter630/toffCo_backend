package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.monitoring.MessageLogService;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatBotServiceTest {
    @Mock WhatsappSessionStore sessionStore;
    @Mock WhatzapService evolutionApiClient;
    @Mock MessageLogService messageLog;

    @Test
    void menuPrincipalTemAsQuatroOpcoesNovas() {
        String number = "5511999999999";
        WhatsappSession session = WhatsappSession.newSession(number);
        when(sessionStore.markMessageAsProcessed("m1", number, "oi")).thenReturn(true);
        when(sessionStore.findByWhatsappId(number)).thenReturn(Optional.of(session));
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog);
        String response = service.simulateIncomingMessage(number, "oi", "m1");
        assertTrue(response.contains("Comprar produtos no site"));
        assertTrue(response.contains("Manutenção de impressoras 3D"));
        assertTrue(response.contains("Consultoria em impressão 3D"));
        assertTrue(response.contains("Falar com um atendente"));
    }

    @Test
    void opcaoDoisPedeDescricaoEPreparaAtendimento() {
        String number = "5511999999999";
        WhatsappSession session = new WhatsappSession(number, ChatState.MENU_PRINCIPAL, 1, false,
                null, null, null, null, null, null, null, null);
        when(sessionStore.markMessageAsProcessed("m2", number, "2")).thenReturn(true);
        when(sessionStore.findByWhatsappId(number)).thenReturn(Optional.of(session));
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog);
        String response = service.simulateIncomingMessage(number, "2", "m2");
        assertEquals(ChatState.DESCRICAO_ATENDIMENTO, session.getCurrentState());
        assertEquals("Manutenção de impressoras 3D", session.getAttendanceSubject());
        assertTrue(response.contains("Descreva o que precisa"));
    }

    @Test
    void mensagemDuplicadaNaoEhProcessadaDuasVezes() {
        String number = "5511999999999";
        when(sessionStore.markMessageAsProcessed("duplicada", number, "1")).thenReturn(false);
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog);
        assertNull(service.simulateIncomingMessage(number, "1", "duplicada"));
        verify(sessionStore, never()).save(any());
        verifyNoInteractions(evolutionApiClient);
    }

    @Test
    void respostaDoBotNaoEhInterpretadaComoIntervencaoHumana() {
        String number = "5511999999999";
        WhatsappSession session = WhatsappSession.newSession(number);

        when(sessionStore.isResponseDuplicate(number, "menu"))
                .thenReturn(false);
        when(sessionStore.findByWhatsappId(number))
                .thenReturn(Optional.of(session));
        when(evolutionApiClient.sendMessage(any()))
                .thenReturn(true);

        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog);

        assertTrue(service.sendResponseClient(number, "menu"));
        service.handlePossibleHumanIntervention(number);

        assertFalse(session.isHumanAssigned());
        assertNotNull(session.getLastBotReplyAt());
    }
}
