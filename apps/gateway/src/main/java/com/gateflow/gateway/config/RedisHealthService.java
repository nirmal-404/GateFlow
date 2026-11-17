
package com.gateflow.gateway.config;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class RedisHealthService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisHealthService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> checkConnection() {
        String key = "gateflow:health-check";

        return redisTemplate.opsForValue()
                .set(key, "connected")
                .then(redisTemplate.opsForValue().get(key))
                .map("connected"::equals)
                .defaultIfEmpty(false);
    }
}
