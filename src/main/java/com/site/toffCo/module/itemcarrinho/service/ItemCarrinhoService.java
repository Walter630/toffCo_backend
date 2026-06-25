package com.site.toffCo.module.itemcarrinho.service;

import com.site.toffCo.module.itemcarrinho.repository.ItemCarrinhoRepository;
import org.springframework.stereotype.Service;

@Service
public class ItemCarrinhoService {

    private final ItemCarrinhoRepository repository;

    public ItemCarrinhoService(ItemCarrinhoRepository repository) {
        this.repository = repository;
    }
}
