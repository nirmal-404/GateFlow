package com.gateflow.gateway.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class RedisHealthController {

    private final RedisHealthService redisHealthService;

    public RedisHealthController(RedisHealthService redisHealthService) {
        this.redisHealthService = redisHealthService;
    }

    @GetMapping("/internal/redis-health")
    public Mono<String> checkRedis() {
        return redisHealthService.checkConnection()
                .map(connected -> connected
                        ? "Redis connection successful"
                        : "Redis connection failed");
    }
}
