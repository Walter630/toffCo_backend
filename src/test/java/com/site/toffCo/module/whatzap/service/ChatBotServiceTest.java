package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.infra.config.WhatsappProperties;
import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.monitoring.BlocklistWatchdog;
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
    @Mock WhatsappProperties whatsappProperties;
    @Mock BlocklistWatchdog blocklistWatchdog;

    @Test
    void menuPrincipalTemAsQuatroOpcoesNovas() {
        String number = "5511999999999";
        WhatsappSession session = WhatsappSession.newSession(number);
        when(sessionStore.markMessageAsProcessed("m1", number, "oi")).thenReturn(true);
        when(sessionStore.findByWhatsappId(number)).thenReturn(Optional.of(session));
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog, whatsappProperties, blocklistWatchdog);
        String response = service.simulateIncomingMessage(number, "oi", "m1");
        assertTrue(response.contains("Manutenção e revisão em impressoras"));
        assertTrue(response.contains("Consultoria, mentoria e cursos"));
        assertTrue(response.contains("Compras em Atacado"));
        assertTrue(response.contains("Dúvidas/atendimento"));
    }

    @Test
    void opcaoDoisEncaminhaDiretamenteParaAtendimento() {
        String number = "5511999999999";
        WhatsappSession session = new WhatsappSession(number, ChatState.MENU_PRINCIPAL, 1, false,
                null, null, null, null, null, null, null, null);
        when(sessionStore.markMessageAsProcessed("m2", number, "2")).thenReturn(true);
        when(sessionStore.findByWhatsappId(number)).thenReturn(Optional.of(session));
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog, whatsappProperties, blocklistWatchdog);
        String response = service.simulateIncomingMessage(number, "2", "m2");
        assertEquals(ChatState.ATENDIMENTO_HUMANO, session.getCurrentState());
        assertEquals("Consultoria, mentoria e cursos", session.getAttendanceSubject());
        assertTrue(session.isHumanAssigned());
        assertNull(response);
    }

    @Test
    void mensagemDuplicadaNaoEhProcessadaDuasVezes() {
        String number = "5511999999999";
        when(sessionStore.markMessageAsProcessed("duplicada", number, "1")).thenReturn(false);
        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog, whatsappProperties, blocklistWatchdog);
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

        ChatBotService service = new ChatBotService(sessionStore, evolutionApiClient, messageLog, whatsappProperties, blocklistWatchdog);

        assertTrue(service.sendResponseClient(number, "menu"));
        service.handlePossibleHumanIntervention(number);

        assertFalse(session.isHumanAssigned());
        assertNotNull(session.getLastBotReplyAt());
    }
}
