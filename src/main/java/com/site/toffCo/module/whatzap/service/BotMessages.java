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
    /*public static String formatDynamicCatalog(
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
    }*/

    public static final String WELCOME_MENU =
            "Olá! 👋 Seja muito bem-vindo à *Toff Brasil*!\n\n" +
                    "Como posso te ajudar hoje? Escolha uma das opções abaixo:\n\n" +
                    "1️⃣ *Catálogo de Filamentos*\n" +
                    "2️⃣ *Catálogo de Produtos*\n" +
                    "3️⃣ *Máquinas*\n" +
                    "4️⃣ *Acessórios*\n" +
                    "5️⃣ *Impressoras 3D*\n" +
                    "6️⃣ *Falar com um atendente*\n\n" +
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
                    "3️⃣ *Máquinas*\n" +
                    "4️⃣ *Acessórios*\n" +
                    "5️⃣ *Impressoras 3D*\n" +
                    "6️⃣ *Falar com um atendente*\n\n" +
                    "_Responda apenas com o número da opção desejada._";

    public static String getProductLink(String productName, int page) {
        return "Excelente escolha! 🌟\n\n" +
                "Você selecionou o *" + productName + "* (Página " + page + ").\n\n" +
                "🔗 *Confira todos os detalhes abaixo:*\n" +
                "https://toffbr.com.br/dashboard";
    }

    public static String getCatalogLink(String productName) {
        String[] info =  CATEGORIAS.getOrDefault(productName.toUpperCase(), new String[]{"produtos", "Produtos"});
        String slug = info[0];
        String tituloAmigavel = info[1];

        return "📦 Confira nosso catálogo completo de *" + tituloAmigavel + "* no site:\n\n" +
                "🔗 https://toffbr.com.br/catalogo?categoria=" + slug + "\n\n" +
                "⬅️ 7 - Voltar ao Menu";
    }
}
