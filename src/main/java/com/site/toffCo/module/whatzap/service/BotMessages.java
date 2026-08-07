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
            "Bem-vindo (a) á *Toff*!\n\n" +
            "Sou assistente virtual. Como posso ajudar?\n\n" +
             "*Se o seu interesse é preço, disponibilidade de estoque e marcas que trabalhamos, acesse o site abaixo* \n" + "https://toffbr.com.br/catalogo \n\n" +
             "Agora se você não quer nada relacionado acima, me diz abaixo o que você busca: \n" +
            "1️⃣ Manutenção e revisão em impressoras\n" +
            "2️⃣ Consultoria, mentoria e cursos\n" +
            "3️⃣ Compras em Atacado\n" +
            "4️⃣ Dúvidas/atendende\n\n" +
            "Digite o número da opção.";

    public static final String ATTENDANCE_SUBJECT_MENU =
            "Qual assunto? \n\n" +
            "1️⃣ Mentoria\n" +
            "2️⃣ Manutenção\n" +
            "3️⃣ Dúvida sobre produto/máquina\n" +
            "4️⃣ Outro assunto\n" +
            "0️⃣ Voltar ao menu";

    public static String askProblemDescription(String subject) {
        return "Assunto: *" + subject + "*.\n\n" +
                "Descreva o que precisa em uma mensagem de texto.";
    }

    public static String askAtacado(String subject) {
        return "Assunto: *" + subject + "*.\n\n" + "Nosso atacado é a partir de 30kg de filamentos, podendo variar entre cores, marcas e tipos.\n" +
                "Caso queira dar sequência, como prefere seguir?  \n\n" +
                "1️⃣ Quero receber um orçamento \n" +
                "2️⃣ Tenho dúvidas antes de fechar \n\n" +
                "Caso queira voltar ao menu digite o numero abaixo: \n" +
                "0️⃣ Voltar ao menu";
    }

    public static final String HUMAN_ATTENDANCE =
            "Certo! Seu atendimento foi encaminhado.\n\n" +
            "Nossa equipe responderá por aqui.";

    public static final String BACK_TO_MENU = "Voltando ao menu. 🔙\n\n" + WELCOME_MENU;

    public static final String INVALID_OPTION = "Opção inválida. Digite um número do menu.";

    public static final String SYSTEM_FAILURE =
            "Tivemos uma instabilidade momentânea. Um atendente foi acionado.\n\n" +
            "Você também pode consultar nossos produtos:\n" +
            "https://toffbr.com.br/catalogo";

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
            "Atendimento encaminhado. Nossa equipe responderá por aqui.\n\n";
}
