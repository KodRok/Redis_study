package com.sorokin.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisLockManager {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end",
            Long.class
    );

    public boolean tryLock(String key, String token, Duration ttl) {
        log.info("Lock attempt: key={}, token={}, ttl={}ms", key, token, ttl.toMillis());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            log.info("Lock acquired: key={}, token={}", key, token);
            return true;
        } else {
            log.warn("Lock rejected (already locked): key={}, token={}", key, token);
            return false;
        }
    }

    public boolean unlock(String key, String token) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        boolean released = Long.valueOf(1L).equals(result);
        if (released) {
            log.info("Lock successfully released: key={}, token={}", key, token);
        } else {
            log.warn("Lock release failed (expired or wrong token): key={}, token={}", key, token);
        }
        return released;
    }

}