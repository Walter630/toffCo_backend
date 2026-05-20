package com.site.toffCo.module.auth.service;

import com.site.toffCo.module.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    public CleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanup() {
        refreshTokenRepository.deleteByExpiryDate(Instant.now());
    }
}
