package com.ota.flights.domain.port;

import java.util.Optional;

public interface CachePort {

    Optional<byte[]> get(String key);

    void set(String key, byte[] value, int ttlSeconds);

    void deletePattern(String pattern);
}
