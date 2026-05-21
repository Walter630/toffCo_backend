package com.site.toffCo.infra.redis;

import com.site.toffCo.module.carrinho.dto.CarrinhoResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CarrinhoCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void salvarCarrinho(String userId, CarrinhoResponseDTO carrinhoResponseDTO) {
        //salva com expiraçao de 1 hora ou menos
        redisTemplate.opsForValue().set("carrinho" + userId, carrinhoResponseDTO, Duration.ofHours(1));
    }

    public CarrinhoResponseDTO getCache(String userId) {
        return (CarrinhoResponseDTO) redisTemplate.opsForValue().get("carrinho" + userId);
    }

    public void removerCache(String userId) {
        redisTemplate.delete("carrinho" + userId);
    }
}
