package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.produto.entity.Produto;

import java.util.List;

public class BotMessages {

    public static String formatDynamicCatalog(
            String titulo,
            List<Produto> produtos,
            int pagina,
            boolean hasNextPage
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 *").append(titulo).append(" | Página ").append(pagina).append("*\n\n");

        for (int i = 0; i < produtos.size(); i++) {
            Produto produto = produtos.get(i);
            sb.append(i + 1)
                    .append(" - ")
                    .append(produto.getName())
                    .append(" (R$ ")
                    .append(produto.getPrice())
                    .append(")\n");
        }

        if (hasNextPage) {
            sb.append("\n➡️ 4 - Próxima página\n");
        }

        sb.append("⬅️ 5 - Voltar ao Menu");
        return sb.toString();
    }

    public static final String WELCOME_MENU =
            "Olá! 👋 Seja muito bem-vindo à *ToffCo*!\n\n" +
                    "Como posso te ajudar hoje? Escolha uma das opções abaixo:\n\n" +
                    "1️⃣ *Catálogo de Filamentos*\n" +
                    "2️⃣ *Catálogo de Produtos*\n" +
                    "3️⃣ *Falar com um atendente*\n\n" +
                    "_Responda apenas com o número da opção desejada._";

    public static final String HUMAN_ATTENDANCE =
            "Entendido! ✅\n\n" +
                    "Seu atendimento já foi encaminhado para nossa equipe. Um de nossos especialistas entrará em contato por aqui mesmo em instantes.\n\n" +
                    "_Agradecemos a paciência!_ 🙏";

    public static final String BACK_TO_MENU =
            "Voltando... 🔙\n\n" +
                    "Olá! 👋 Seja muito bem-vindo à *ToffCo*!\n\n" +
                    "Como posso te ajudar hoje? Escolha uma das opções abaixo:\n\n" +
                    "1️⃣ *Catálogo de Filamentos*\n" +
                    "2️⃣ *Catálogo de Produtos*\n" +
                    "3️⃣ *Falar com um atendente*\n\n" +
                    "_Responda apenas com o número da opção desejada._";

    public static String getProductLink(String productName, int page) {
        return "Excelente escolha! 🌟\n\n" +
                "Você selecionou o *" + productName + "* (Página " + page + ").\n\n" +
                "🔗 *Confira todos os detalhes abaixo:*\n" +
                "https://seusistema.com";
    }
}
