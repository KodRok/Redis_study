package com.sorokin.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisConfig {

//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//        // Ключи храним как обычные строки (например, "product:1")
//        template.setKeySerializer(new StringRedisSerializer());
//        // Значения храним как JSON
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        return template;
//    }


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2)) // 1. Задаем общий TTL для всех кэшей
                .disableCachingNullValues()      // 2. Не кэшируем null, чтобы избежать ошибок
                .serializeValuesWith(            // 3. Превращаем объекты в читаемый JSON
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();

    }
}
