package com.gateflow.gateway.apikey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.gateflow.gateway.apikey.exception.RedisRateLimitException;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class RedisRateLimitServiceTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private RedisRateLimitService rateLimitService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = org.mockito.Mockito.mock(
                ReactiveStringRedisTemplate.class
        );

        rateLimitService = new RedisRateLimitService(redisTemplate);
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        mockRedisResult(List.of(1L, 60L));

        StepVerifier.create(
                rateLimitService.checkRateLimit(
                        UUID.randomUUID(), 100, 60
                )
        )
                .assertNext(result -> {
                    assertTrue(result.allowed());
                    assertEquals(1, result.currentRequests());
                    assertEquals(99, result.remainingRequests());
                    assertEquals(Duration.ofSeconds(60), result.retryAfter());
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectRequestWhenRateLimitExceeded() {
        mockRedisResult(List.of(101L, 30L));

        StepVerifier.create(
                rateLimitService.checkRateLimit(
                        UUID.randomUUID(), 100, 60
                )
        )
                .assertNext(result -> {
                    assertFalse(result.allowed());
                    assertEquals(101, result.currentRequests());
                    assertEquals(0, result.remainingRequests());
                    assertEquals(Duration.ofSeconds(30), result.retryAfter());
                })
                .verifyComplete();
    }

    @Test
    void shouldUseMinimumOneSecondRetryDuration() {
        mockRedisResult(List.of(101L, 0L));

        StepVerifier.create(
                rateLimitService.checkRateLimit(
                        UUID.randomUUID(), 100, 60
                )
        )
                .assertNext(result
                        -> assertEquals(
                                Duration.ofSeconds(1),
                                result.retryAfter()
                        )
                )
                .verifyComplete();
    }

    @Test
    void shouldWrapRedisFailure() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<List>>any(),
                anyList(),
                anyString()
        )).thenReturn(Flux.error(
                new RuntimeException("Redis unavailable")
        ));

        StepVerifier.create(
                rateLimitService.checkRateLimit(
                        UUID.randomUUID(), 100, 60
                )
        )
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(
                            RedisRateLimitException.class,
                            error
                    );

                    assertEquals(
                            "Unable to evaluate API rate limit",
                            error.getMessage()
                    );
                })
                .verify();
    }

    @Test
    void shouldRejectEmptyRedisResult() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<List>>any(),
                anyList(),
                anyString()
        )).thenReturn(Flux.empty());

        StepVerifier.create(
                rateLimitService.checkRateLimit(
                        UUID.randomUUID(), 100, 60
                )
        )
                .expectError(RedisRateLimitException.class)
                .verify();
    }

    @SuppressWarnings("unchecked")
    private void mockRedisResult(List<Long> result) {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<List>>any(),
                anyList(),
                anyString()
        )).thenReturn(Flux.just(result));
    }
}
