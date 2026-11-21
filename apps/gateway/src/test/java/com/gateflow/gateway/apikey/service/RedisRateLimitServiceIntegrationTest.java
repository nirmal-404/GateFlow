package com.gateflow.gateway.apikey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.gateflow.gateway.apikey.model.RateLimitResult;

import reactor.core.publisher.Flux;

@Testcontainers
class RedisRateLimitServiceIntegrationTest {

    @Container
    static final GenericContainer<?> redis
            = new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisRateLimitService rateLimitService;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                redis.getHost(),
                redis.getMappedPort(6379)
        );

        connectionFactory.afterPropertiesSet();

        ReactiveStringRedisTemplate redisTemplate
                = new ReactiveStringRedisTemplate(connectionFactory);

        rateLimitService = new RedisRateLimitService(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldEnforceRateLimitUnderConcurrentRequests() {
        UUID apiKeyId = UUID.randomUUID();

        int totalRequests = 50;
        int rateLimit = 10;
        int windowSeconds = 60;

        List<RateLimitResult> results = Flux.range(0, totalRequests)
                .flatMap(index
                        -> rateLimitService.checkRateLimit(
                                apiKeyId,
                                rateLimit,
                                windowSeconds
                        ),
                        totalRequests
                )
                .collectList()
                .block(Duration.ofSeconds(20));

        assertNotNull(results);
        assertEquals(totalRequests, results.size());

        long allowedRequests = results.stream()
                .filter(RateLimitResult::allowed)
                .count();

        long rejectedRequests = results.stream()
                .filter(result -> !result.allowed())
                .count();

        assertEquals(rateLimit, allowedRequests);
        assertEquals(totalRequests - rateLimit, rejectedRequests);

        long highestRequestCount = results.stream()
                .mapToLong(RateLimitResult::currentRequests)
                .max()
                .orElseThrow();

        assertEquals(totalRequests, highestRequestCount);
    }
}
