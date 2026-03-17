package com.example.echoshotx.shared.security.service;

import com.example.echoshotx.shared.redis.service.RedisService;
import com.example.echoshotx.shared.security.domain.WebhookNonceRecord;
import com.example.echoshotx.shared.security.infrastructure.WebhookNonceRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookReplayGuardService {

    private static final String NONCE_PREFIX = "webhook:nonce:";
    private static final Duration NONCE_TTL = Duration.ofMinutes(5);

    private final RedisService redisService;
    private final WebhookNonceRepository nonceRepository;

    @Transactional
    public boolean registerNonce(String nonce) {
        String key = NONCE_PREFIX + nonce;

        try {
            if (!redisService.setIfAbsent(key, "1", NONCE_TTL)) {
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("Redis nonce registration failed. fallback to DB. nonce={}", nonce, e);
        }

        if (nonceRepository.existsByNonce(nonce)) {
            return false;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            nonceRepository.save(WebhookNonceRecord.create(nonce, now, now.plus(NONCE_TTL)));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void cleanupExpiredNonces() {
        nonceRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
