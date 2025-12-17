package com.example.echoshotx.shared.redis.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String AUTH_CODE_PREFIX = "auth:code:";
    private static final String CLIENT_TYPE_PREFIX = "auth:client:";

    public void setToken(String token, String username) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(token, username, Duration.ofDays(7));
    }

    public String getToken(String token) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(token);
    }

    public void deleteToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            // "Bearer " 접두사 제거
            token = token.substring(7);
        }
        redisTemplate.delete(token);
    }

    /**
     * 1회용 인증 코드 저장
     */
    public void setAuthCode(String code, String username, Duration ttl) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String key = AUTH_CODE_PREFIX + code;
        values.set(key, username, ttl);
        log.info("Auth code saved: {}", code);
    }

    /**
     * 인증 코드로 username 조회
     */
    public String getAuthCode(String code) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String key = AUTH_CODE_PREFIX + code;
        return values.get(key);
    }

    /**
     * 인증 코드 삭제 (1회 사용 보장)
     */
    public void deleteAuthCode(String code) {
        String key = AUTH_CODE_PREFIX + code;
        redisTemplate.delete(key);
        log.info("Auth code deleted: {}", code);
    }

    /**
     * OAuth2 state와 client 타입 매핑 저장
     */
    public void setClientType(String state, String clientType, Duration ttl) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String key = CLIENT_TYPE_PREFIX + state;
        values.set(key, clientType, ttl);
        log.info("Client type saved for state: {} -> {}", state, clientType);
    }

    /**
     * OAuth2 state로 client 타입 조회
     */
    public String getClientType(String state) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        String key = CLIENT_TYPE_PREFIX + state;
        return values.get(key);
    }
}
