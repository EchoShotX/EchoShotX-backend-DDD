package com.example.echoshotx.shared.redis.service;

import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryLock(String key, String token, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String key, String token) {
        DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>();
        unlockScript.setScriptText(UNLOCK_LUA_SCRIPT);
        unlockScript.setResultType(Long.class);

        redisTemplate.execute(unlockScript, Collections.singletonList(key), token);
    }
}
