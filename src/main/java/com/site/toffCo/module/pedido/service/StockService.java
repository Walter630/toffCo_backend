package com.site.toffCo.module.pedido.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    // private final ProdutoRepository produtoRepository;

    @Transactional
    public void deductStockFromPresencialSale(String sku, int quantity) {
        // Lógica de banco de dados clássica:
        // 1. Busca o produto pelo SKU
        // 2. Subtrai a quantidade vendida no presencial do estoque local
        // 3. Salva a alteração
        System.out.println("Baixando " + quantity + " unidades do produto " + sku + " no banco de dados local.");
    }
}
