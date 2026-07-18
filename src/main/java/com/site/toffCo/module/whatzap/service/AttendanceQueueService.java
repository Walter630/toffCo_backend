package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.ChatState;
import com.site.toffCo.module.whatzap.dto.ChatStatus;
import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import com.site.toffCo.module.whatzap.session.WhatsappSession;
import com.site.toffCo.module.whatzap.session.WhatsappSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceQueueService {

    private final WhatsappSessionStore sessionStore;
    private final WhatzapService evolutionApiClient;

    // ─── DASHBOARD / API ─────────────────────────────────────────

    public List<WhatsappSession> getPendingQueue() {
        return sessionStore.findAll().stream()
                .filter(s -> s.getCurrentState() == ChatState.ATENDIMENTO_HUMANO)
                .filter(s -> s.getStatus() == ChatStatus.PENDING || s.getStatus() == ChatStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(
                        WhatsappSession::getHumanAssingnedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public boolean assignToAttendant(String clientWhatsappId, String attendantWhatsappId) {
        Optional<WhatsappSession> opt = sessionStore.findByWhatsappId(clientWhatsappId);
        if (opt.isEmpty()) return false;

        WhatsappSession session = opt.get();
        if (session.getStatus() != ChatStatus.PENDING) return false;

        session.setStatus(ChatStatus.IN_PROGRESS);
        session.setAssignedTo(attendantWhatsappId);
        sessionStore.save(session);
        return true;
    }

    public boolean releaseSession(String clientWhatsappId) {
        Optional<WhatsappSession> opt = sessionStore.findByWhatsappId(clientWhatsappId);
        if (opt.isEmpty()) return false;

        WhatsappSession session = opt.get();
        session.setHumanAssigned(false);
        session.setCurrentState(ChatState.MENU_PRINCIPAL);
        session.setStatus(ChatStatus.RESOLVED);
        session.setAssignedTo(null);
        session.setAttendanceSubject(null);
        sessionStore.save(session);

        // Avisa o cliente que pode usar o bot de novo
        evolutionApiClient.sendMessage(new SendMessageRequest(
                clientWhatsappId,
                "✅ Atendimento finalizado!\n\nSe precisar de mais alguma coisa, é só enviar *menu*.",
                2200
        ));

        return true;
    }

    // ─── COMANDOS DO WHATSAPP DO ATENDENTE ───────────────────────

    public String handleAttendantCommand(String attendantNumber, String commandText) {
        String[] parts = commandText.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : null;

        return switch (cmd) {
            case "/pendentes" -> listPendingForAttendant();
            case "/atender" -> arg != null ? assignFromWhatsApp(arg, attendantNumber) : "❌ Use: /atender 5511999999999";
            case "/finalizar", "/liberar" -> arg != null ? releaseFromWhatsApp(arg) : "❌ Use: /finalizar 5511999999999";
            case "/info" -> arg != null ? showClientInfo(arg) : "❌ Use: /info 5511999999999";
            case "/resumo" -> showAttendantSummary(attendantNumber);
            default -> null; // não é comando, ignora
        };
    }

    private String listPendingForAttendant() {
        List<WhatsappSession> pending = getPendingQueue().stream()
                .filter(s -> s.getStatus() == ChatStatus.PENDING)
                .limit(5)
                .toList();

        if (pending.isEmpty()) return "✅ Nenhum atendimento pendente na fila.";

        StringBuilder sb = new StringBuilder("📋 *Fila de Atendimentos*\n\n");
        for (WhatsappSession s : pending) {
            long minutos = s.getHumanAssingnedAt() != null
                    ? Duration.between(s.getHumanAssingnedAt(), Instant.now()).toMinutes()
                    : 0;

            sb.append("• `").append(s.getWhatsappId()).append("`")
                    .append("\n  ⏱ ").append(minutos).append(" min na fila")
                    .append("\n  🏷 ").append(s.getAttendanceSubject() != null ? s.getAttendanceSubject() : "Sem assunto")
                    .append("\n  💬 _").append(truncate(s.getLastMessage(), 40)).append("_")
                    .append("\n\n");
        }
        sb.append("Pra pegar: `/atender 5511999999999`");
        return sb.toString();
    }

    private String assignFromWhatsApp(String clientNumber, String attendantNumber) {
        String clean = clientNumber.replaceAll("\\D", "");
        if (!assignToAttendant(clean, attendantNumber)) {
            return "❌ Número não encontrado na fila ou já está em atendimento.";
        }
        return "✅ Você pegou o atendimento de " + clean + ".\n\n" +
                "Acesse o chat dele no WhatsApp e responda. Quando terminar: `/finalizar " + clean + "`";
    }

    private String releaseFromWhatsApp(String clientNumber) {
        String clean = clientNumber.replaceAll("\\D", "");
        if (!releaseSession(clean)) {
            return "❌ Não consegui liberar. Verifique o número.";
        }
        return "✅ Atendimento de " + clean + " finalizado. Bot voltou a responder.";
    }

    private String showClientInfo(String clientNumber) {
        String clean = clientNumber.replaceAll("\\D", "");
        Optional<WhatsappSession> opt = sessionStore.findByWhatsappId(clean);
        if (opt.isEmpty()) return "❌ Cliente não encontrado.";

        WhatsappSession s = opt.get();
        long minutos = s.getHumanAssingnedAt() != null
                ? Duration.between(s.getHumanAssingnedAt(), Instant.now()).toMinutes()
                : 0;

        return "📋 *Info do cliente*\n" +
                "Número: `" + s.getWhatsappId() + "`\n" +
                "Assunto: " + (s.getAttendanceSubject() != null ? s.getAttendanceSubject() : "-") + "\n" +
                "Status: " + s.getStatus() + "\n" +
                "Tempo na fila: " + minutos + " min\n" +
                "Última msg: _" + truncate(s.getLastMessage(), 50) + "_";
    }

    private String showAttendantSummary(String attendantNumber) {
        List<WhatsappSession> mine = sessionStore.findAll().stream()
                .filter(s -> attendantNumber.equals(s.getAssignedTo()))
                .filter(s -> s.getStatus() == ChatStatus.IN_PROGRESS)
                .toList();

        long pending = sessionStore.findAll().stream()
                .filter(s -> s.getStatus() == ChatStatus.PENDING)
                .count();

        return "📊 *Seu resumo*\n" +
                "Você está atendendo: " + mine.size() + " conversa(s)\n" +
                "Pendentes na fila: " + pending + "\n\n" +
                (mine.isEmpty() ? "" : "Seus atendimentos:\n" +
                        mine.stream().map(s -> "• " + s.getWhatsappId()).reduce((a, b) -> a + "\n" + b).orElse(""));
    }

    private String truncate(String text, int max) {
        if (text == null) return "-";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}