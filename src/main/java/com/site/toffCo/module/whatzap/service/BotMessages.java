package com.site.toffCo.module.whatzap.service;

import java.util.Map;

public class BotMessages {

    private static final Map<String, String[]> CATEGORIAS = Map.of(
            "FILAMENTOS", new String[]{"filamentos", "Filamentos"},
            "PRODUTOS", new String[]{"produtos", "Produtos"},
            //"MAQUINAS", new String[]{"maquinas", "Máquinas"},
            "ACESSORIOS", new String[]{"acessorios", "Acessórios"},
            "IMPRESSORAS", new String[]{"impressoras", "Impressoras 3D"}
    );

    public static final String WELCOME_MENU =
            """
                    Olá! 👋 Seja muito bem-vindo à *Toff Brasil*!
                    
                    Eu sou a *Carol*, atendente virtual da Toff. Vou agilizar seu atendimento por aqui.
                    
                    1️⃣ *Catálogo de Filamentos*
                    2️⃣ *Catálogo de Produtos*
                    3️⃣ *Máquinas*
                    4️⃣ *Acessórios*
                    5️⃣ *Falar com um atendente*
                    
                    _Responda apenas com o número da opção desejada._""";

    public static final String ATTENDANCE_SUBJECT_MENU =
            """
                    Certo! Para eu te direcionar para o atendente certo e agilizar seu atendimento, me informe o assunto:
                    
                    1️⃣ *Mentoria*
                    2️⃣ *Manutenção em máquina*
                    3️⃣ *Compra em atacado acima de 30kg*
                    4️⃣ *Dúvida sobre catálogo, produto ou máquina*
                    5️⃣ *Outro assunto*
                    0️⃣ *Voltar ao menu*
                    
                    _Responda apenas com o número da opção._""";

    public static String askProblemDescription(String subject) {
        return "Perfeito! ✅\n\n" +
                "Assunto selecionado: *" + subject + "*.\n\n" +
                "Agora, em poucas palavras, me conte qual é o problema ou o que você gostaria de resolver.\n\n" +
                "⚠️ Para agilizar, envie sua mensagem em *texto*. No momento, o atendimento virtual não consegue analisar áudio, vídeo ou imagem com precisão.";
    }

    public static final String HUMAN_ATTENDANCE =
            "Obrigado pelas informações! ✅\n\n" +
                    "Já encaminhei seu atendimento para nossa equipe com o resumo do seu caso.\n\n" +
                    "Um de nossos especialistas vai te responder por aqui assim que possível.";

    public static final String BACK_TO_MENU =
            "Tudo bem, voltamos ao menu principal. 🔙\n\n" +
                    WELCOME_MENU;

    public static String getProductLink(String productName, int page) {
        return "Excelente escolha! 🌟\n\n" +
                "Você selecionou o *" + productName + "* (Página " + page + ").\n\n" +
                "🔗 *Confira todos os detalhes abaixo:*\n" +
                "https://toffbr.com.br/dashboard";
    }

    public static final String INVALID_OPTION =
            "Não consegui entender essa opção. 😅\n\n" +
                    "Por favor, responda apenas com o número de uma das opções do menu.";

    public static String getCatalogLink(String productName) {
        String[] info = CATEGORIAS.getOrDefault(productName.toUpperCase(), new String[]{"produtos", "Produtos"});
        String slug = info[0];
        String tituloAmigavel = info[1];

        return "📦 Confira nosso catálogo completo de *" + tituloAmigavel + "* no site:\n\n" +
                "🔗 https://toffbr.com.br/catalogo?categoria=" + slug + "\n\n" +
                "0️⃣ *Voltar ao menu*";
    }

    public static String managerNotification(String whatsappId, String subject, String description) {
        /*
         * Formata o número para exibição legível: 5548999999999 → 55 48 99999-9999
         * Funciona para celular (13 dígitos com DDI) e fixo (12 dígitos).
         * Se o formato não bater, exibe o número original sem quebrar.
         */
        String displayNumber = formatPhoneNumber(whatsappId);
        String waLink = "https://wa.me/" + whatsappId;

        return "🔔 *Novo atendimento na fila!*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n\n" +
                "👤 *Cliente*\n" +
                "   Número: " + displayNumber + "\n" +
                "   Link direto: " + waLink + "\n\n" +
                "🏷 *Assunto*\n" +
                "   " + subject + "\n\n" +
                "💬 *Resumo do cliente*\n" +
                "   _" + description + "_\n\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "➡️ Para atender: `/atender " + whatsappId + "`\n" +
                "📋 Ver fila completa: `/pendentes`";
    }

    private static String formatPhoneNumber(String raw) {
        if (raw == null) return "-";
        String digits = raw.replaceAll("\\D", "");

        // DDI (2) + DDD (2) + 9 dígitos = 13 chars → celular com DDI
        if (digits.length() == 13) {
            return digits.substring(0, 2) + " " +   // DDI: 55
                   digits.substring(2, 4) + " " +   // DDD: 48
                   digits.substring(4, 9) + "-" +   // prefixo: 99999
                   digits.substring(9);              // sufixo: 9999
        }
        // DDI (2) + DDD (2) + 8 dígitos = 12 chars → fixo com DDI
        if (digits.length() == 12) {
            return digits.substring(0, 2) + " " +
                   digits.substring(2, 4) + " " +
                   digits.substring(4, 8) + "-" +
                   digits.substring(8);
        }
        return raw; // formato desconhecido, exibe como veio
    }

    public static final String WAITING_ATTENDANT_WITH_LINK =
            "Obrigado por me explicar! ✅\n\n" +
                    "Já encaminhei seu atendimento para nossa equipe com o resumo do seu caso.\n\n" +
                    "Enquanto um especialista não responde, você pode consultar nosso catálogo por aqui:\n\n" +
                    "🔗 https://toffbr.com.br/catalogo\n\n" +
                    "Assim que possível, alguém da nossa equipe continua o atendimento com você.\n\n" +
                    "Caso queira voltar para falar com a *karol* (bot), Digite *Menu*!!";

}
