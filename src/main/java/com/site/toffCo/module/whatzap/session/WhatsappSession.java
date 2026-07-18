package com.site.toffCo.module.whatzap.session;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.ChatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class WhatsappSession {

    private String whatsappId;
    private ChatState currentState;
    private int currentPage;
    private boolean humanAssigned;
    private String lastMessageId;
    private Instant lastBotReplyAt;
    private String AttendanceSubject;

    // ─── CAMPOS NOVOS PRA FILA ─────────────────────────────

    private Instant humanAssingnedAt;
    private String lastMessage;
    private String assignedTo;
    private ChatStatus status;
    private String resolvedBy;

    public static WhatsappSession newSession(String whatsappId) {
        return new WhatsappSession(
                whatsappId,
                ChatState.MENU_PRINCIPAL,
                1,
                false,
                null,
                null,
                null,
                // VALORES NOVOS
                null,
                null,
                null,
                null,
                null
        );
    }
}
