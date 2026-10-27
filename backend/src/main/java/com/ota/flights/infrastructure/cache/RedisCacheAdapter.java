package com.ota.flights.infrastructure.cache;

import com.ota.flights.domain.port.CachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Component
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;

    public RedisCacheAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<byte[]> get(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? Optional.empty() : Optional.of(v.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void set(String key, byte[] value, int ttlSeconds) {
        redis.opsForValue().set(key, new String(value, StandardCharsets.UTF_8), Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void deletePattern(String pattern) {
        Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
