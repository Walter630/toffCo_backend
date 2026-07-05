package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.produto.entity.Produto;

import java.util.List;
import java.util.Map;

public class BotMessages {

    private static final Map<String, String[]> CATEGORIAS = Map.of(
            "FILAMENTOS", new String[]{"filamentos", "Filamentos"},
            "PRODUTOS", new String[]{"produtos", "Produtos"},
            "MAQUINAS", new String[]{"maquinas", "Máquinas"},
            "ACESSORIOS", new String[]{"acessorios", "Acessórios"},
            "IMPRESSORAS", new String[]{"impressoras", "Impressoras 3D"}
    );

    public static final String WELCOME_MENU =
            "Olá! 👋 Seja muito bem-vindo à *Toff Brasil*!\n\n" +
                    "Eu sou o *Alex*, atendente virtual da Toff. Vou agilizar seu atendimento por aqui.\n\n" +
                    "1️⃣ *Catálogo de Filamentos*\n" +
                    "2️⃣ *Catálogo de Produtos*\n" +
                    "3️⃣ *Máquinas*\n" +
                    "4️⃣ *Acessórios*\n" +
                    "5️⃣ *Impressoras 3D*\n" +
                    "6️⃣ *Falar com um atendente*\n\n" +
                    "_Responda apenas com o número da opção desejada._";

    public static final String ATTENDANCE_SUBJECT_MENU =
            "Certo! Para eu te direcionar para o atendente certo e agilizar seu atendimento, me informe o assunto:\n\n" +
                    "1️⃣ *Mentoria*\n" +
                    "2️⃣ *Manutenção em máquina*\n" +
                    "3️⃣ *Compra em atacado acima de 30kg*\n" +
                    "4️⃣ *Dúvida sobre catálogo, produto ou máquina*\n" +
                    "5️⃣ *Outro assunto*\n" +
                    "0️⃣ *Voltar ao menu*\n\n" +
                    "_Responda apenas com o número da opção._";

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
        return "🔔 *Novo pedido de atendimento!*\n\n" +
                "Cliente: " + whatsappId + "\n" +
                "Assunto: " + subject + "\n\n" +
                "Resumo enviado pelo cliente:\n" +
                description;
    }

    public static final String WAITING_ATTENDANT_WITH_LINK =
            "Obrigado por me explicar! ✅\n\n" +
                    "Já encaminhei seu atendimento para nossa equipe com o resumo do seu caso.\n\n" +
                    "Enquanto um especialista não responde, você pode consultar nosso catálogo por aqui:\n\n" +
                    "🔗 https://toffbr.com.br/catalogo\n\n" +
                    "Assim que possível, alguém da nossa equipe continua o atendimento com você.";

}
