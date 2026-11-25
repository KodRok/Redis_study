package com.sorokin.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final RedisLockManager lockManager;
    //private final RedisTemplate<String, Object> redisTemplate;
    private static final String NULL_MARKER = "EMPTY";
    private static final Duration NULL_TTL = Duration.ofSeconds(15);


    private static final String KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(2); // TTL = 2 минуты

    public Product create(Product product) {
        return repository.save(product);
    }

    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        log.info("DB fetch (cache_miss) for product id={}", id);
        simulateSlowDatabase();
        return repository.findById(id).
                orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public String purchase(Long id) {
        String lockKey = "lock:product:" + id;
        String token = UUID.randomUUID().toString();

        if (!lockManager.tryLock(lockKey, token, Duration.ofSeconds(10))) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Product is currently locked by another request");
        }

        try {
            log.info("Processing purchase for product id={}", id);
            Product product = repository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            // Имитация долгой обработки оплаты (5 секунд)
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return "Purchased: " + product.getName();
        } finally {
            lockManager.unlock(lockKey, token);
        }
    }


//    public Optional<Product> getById(Long id, boolean useCache) {
//        String key = KEY_PREFIX + id;
//
//        // 1. Если режим кэша включен — сначала идем в Redis
//        if (useCache) {
//            Object cached = redisTemplate.opsForValue().get(key);
//            if (NULL_MARKER.equals(cached)) {
//                log.info("cache_hit (null marker) for key={}", key);
//                return Optional.empty();
//            }
//            if (cached instanceof Product product) {
//                log.info("cache_hit for key={}", key);
//                return Optional.of(product);
//            }
//            log.info("cache_miss for key={}", key);
//        } else {
//            log.info("cache_bypassed (useCache=false) for key={}", key);
//        }
//
//        // 2. Имитация тяжелого запроса в БД (1 секунда) и чтение из репозитория
//        simulateSlowDatabase();
//        Optional<Product> productOpt = repository.findById(id);
//        if (useCache) {
//            if (productOpt.isPresent()) {
//                redisTemplate.opsForValue().set(key, productOpt.get(), CACHE_TTL);
//                log.info("saved to cache key={} with TTL={}s", key, CACHE_TTL.getSeconds());
//            } else {
//                redisTemplate.opsForValue().set(key, NULL_MARKER, NULL_TTL);
//                log.info("saved to cache key={} with null_marker={}s", key, NULL_TTL.getSeconds());
//            }
//        }
//
//        return productOpt;
//    }

        @CachePut(value = "products", key = "#id")
        public Product update (Long id, Product updated){
            return repository.findById(id)
                    .map(existing -> {
                        existing.setName(updated.getName());
                        existing.setPrice(updated.getPrice());
                        Product saved = repository.save(existing);
                        log.info("Updated product id={} in DB (cache will be updated by @CachePut)", id);
                        return saved;
                    })
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }

//    public Optional<Product> update(Long id, Product updated) {
//        return repository.findById(id).map(existing -> {
//            existing.setName(updated.getName());
//            existing.setPrice(updated.getPrice());
//            Product saved = repository.save(existing);
//
//            // Инвалидация ключа ПОСЛЕ успешного обновления в БД
//            String key = KEY_PREFIX + id;
//            Boolean deleted = redisTemplate.delete(key);
//            log.info("cache_invalidated (PUT) key={}, existed={}", key, deleted);
//
//            return saved;
//        });
//    }

        @CacheEvict(value = "products", key = "#id")
        public void delete (Long id){
            if (!repository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            repository.deleteById(id);
            log.info("Deleted product id={} from DB (cache will be evicted by @CacheEvict)", id);
        }

//    public boolean delete(Long id) {
//        if (!repository.existsById(id)) {
//            return false;
//        }
//        repository.deleteById(id);
//
//        // Инвалидация ключа ПОСЛЕ успешного удаления из БД
//        String key = KEY_PREFIX + id;
//        Boolean deleted = redisTemplate.delete(key);
//        log.info("cache_invalidated (DELETE) key={}, existed={}", key, deleted);
//
//        return true;
//    }

        private void simulateSlowDatabase () {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
