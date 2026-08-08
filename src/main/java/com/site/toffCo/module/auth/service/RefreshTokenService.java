package com.site.toffCo.module.auth.service;

import com.site.toffCo.infra.exception.user.InvalidRefreshToken;
import com.site.toffCo.module.auth.entity.RefreshToken;
import com.site.toffCo.module.user.entity.User;
import com.site.toffCo.module.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Cria um Refresh Token de 7 dias atrelado ao usuário
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken newToken = new RefreshToken();

        newToken.setUser(user);
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setExpiryDate(
                Instant.now().plus(7, ChronoUnit.DAYS)
        );

        return refreshTokenRepository.save(newToken);
    }


    // Verifica se o token existe e se não está expirado
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate() == null || !token.getExpiryDate().isAfter(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidRefreshToken("Refresh token expirado. Faça login novamente.");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

}