package com.site.toffCo.module.carrinho.service;

import com.site.toffCo.module.carrinho.entity.Carrinho;
import com.site.toffCo.module.carrinho.repository.CarrinhoRepository;
import com.site.toffCo.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CarrinhoCreator {
    private final CarrinhoRepository carrinhoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Carrinho criar(User user){
        Carrinho carrinho = new Carrinho();
        carrinho.setUser(user);
        carrinho.setValorTotal(BigDecimal.ZERO);

        return carrinhoRepository.saveAndFlush(carrinho);
    }
}
