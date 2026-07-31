package com.site.toffCo.module.whatzap.service;

import java.util.Map;

public class BotMessages {

    private static final Map<String, String[]> CATEGORIAS = Map.of(
            "FILAMENTOS", new String[]{"filamentos", "Filamentos"},
            "PRODUTOS", new String[]{"produtos", "Produtos"},
            "ACESSORIOS", new String[]{"acessorios", "Acessórios"},
            "IMPRESSORAS", new String[]{"impressoras", "Impressoras 3D"}
    );

    public static final String WELCOME_MENU =
            "Olá! 👋 Bem-vindo à *Toff Brasil*!\n\n" +
            "Sou a Carol, assistente virtual. Como posso ajudar?\n\n" +
            "1️⃣ Comprar produtos no site\n" +
            "2️⃣ Manutenção de impressoras 3D\n" +
            "3️⃣ Consultoria em impressão 3D\n" +
            "4️⃣ Falar com um atendente\n\n" +
            "Digite o número da opção.";

    public static final String ATTENDANCE_SUBJECT_MENU =
            "Qual assunto?\n\n" +
            "1️⃣ Mentoria\n" +
            "2️⃣ Manutenção\n" +
            "3️⃣ Compra em atacado\n" +
            "4️⃣ Dúvida sobre produto/máquina\n" +
            "5️⃣ Outro assunto\n" +
            "0️⃣ Voltar ao menu";

    public static String askProblemDescription(String subject) {
        return "Assunto: *" + subject + "*.\n\n" +
                "Descreva o que precisa em uma mensagem de texto.";
    }

    public static final String HUMAN_ATTENDANCE =
            "Certo! Seu atendimento foi encaminhado.\n\n" +
            "Nossa equipe responderá por aqui.";

    public static final String BACK_TO_MENU = "Voltando ao menu. 🔙\n\n" + WELCOME_MENU;

    public static final String INVALID_OPTION = "Opção inválida. Digite um número do menu.   \n\n" + WELCOME_MENU;

    public static String getCatalogLink(String productName) {
        String[] info = CATEGORIAS.getOrDefault(productName.toUpperCase(), new String[]{"produtos", "Produtos"});
        return "📦 *" + info[1] + ":*\n" +
                "https://toffbr.com.br/catalogo?categoria=" + info[0] + "\n\n" +
                "Digite 0 para voltar.";
    }

    public static String managerNotification(String whatsappId, String subject, String description) {
        String displayNumber = formatPhoneNumber(whatsappId);
        return "🔔 *Novo atendimento*\n" +
                "Cliente: " + displayNumber + "\n" +
                "Assunto: " + subject + "\n" +
                "Resumo: " + description + "\n\n" +
                "Para atender: `/atender " + whatsappId + "`";
    }

    private static String formatPhoneNumber(String raw) {
        if (raw == null) return "-";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 13) return digits.substring(0, 2) + " " + digits.substring(2, 4) + " " + digits.substring(4, 9) + "-" + digits.substring(9);
        if (digits.length() == 12) return digits.substring(0, 2) + " " + digits.substring(2, 4) + " " + digits.substring(4, 8) + "-" + digits.substring(8);
        return raw;
    }

    public static String unsupportedMediaMessage(String mediaName) {
        return "Recebi " + mediaName + ", mas só consigo ler texto.\n\n" + WELCOME_MENU;
    }

    public static final String WAITING_ATTENDANT_WITH_LINK =
            "Atendimento encaminhado. Nossa equipe responderá por aqui.\n\n" +
            "Para voltar ao bot, digite *menu*.";
}
