package com.ota.flights.infrastructure.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

class RedisCacheAdapterTest {

    @Test
    void putAndGet_delegatesToRedisTemplate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        RedisCacheAdapter adapter = new RedisCacheAdapter(redis);

        adapter.set("k", "v".getBytes(), 60);
        verify(ops).set(eq("k"), eq("v"), any(java.time.Duration.class));
    }
}