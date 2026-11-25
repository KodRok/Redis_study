package com.sorokin.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    // Атомарный Lua-скрипт: инкрементирует счетчик и задает TTL только при hits == 1
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local hits = redis.call('INCR', KEYS[1]) " +
                    "if hits == 1 then " +
                    "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                    "end " +
                    "return hits",
            Long.class
    );


    /**
     * Проверяет, разрешен ли запрос в текущем фиксированном окне.
     *
     * @param clientId          идентификатор клиента (IP или API-ключ)
     * @param maxRequests       лимит запросов в окно (например, 10)
     * @param windowSizeSeconds длительность окна в секундах (например, 60)
     * @return true, если запрос разрешен; false, если лимит превышен
     */
    public boolean isAllowed(String clientId, int maxRequests, int windowSizeSeconds) {
        long nowSeconds = Instant.now().getEpochSecond();
        long windowIndex = nowSeconds / windowSizeSeconds;
        String key = "rate:" + clientId + ":" + windowIndex;

        // Передаем запасной TTL (windowSizeSeconds + 5 сек), чтобы ключ гарантированно дожил до конца окна
        Long hits = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(windowSizeSeconds + 5)
        );

        long currentHits = (hits != null) ? hits : 0L;
        boolean allowed = currentHits <= maxRequests;

        log.info("RateLimit check: clientId={}, windowIndex={}, hits={}, allowed={}",
                clientId, windowIndex, currentHits, allowed);

        return allowed;
    }
}
