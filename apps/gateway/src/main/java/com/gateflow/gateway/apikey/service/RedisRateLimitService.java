package com.gateflow.gateway.apikey.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.RateLimitResult;

import reactor.core.publisher.Mono;

@Service
public class RedisRateLimitService {

    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])

            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end

            local ttl = redis.call('TTL', KEYS[1])

            return {current, ttl}
            """;

    private final ReactiveStringRedisTemplate redisTemplate;

    private final RedisScript<List> rateLimitScript
            = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);

    public RedisRateLimitService(
            ReactiveStringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<RateLimitResult> checkRateLimit(
            UUID apiKeyId,
            int rateLimit,
            int windowSeconds
    ) {
        String redisKey = "gateflow:rate-limit:" + apiKeyId;

        return redisTemplate.execute(
                rateLimitScript,
                List.of(redisKey),
                String.valueOf(windowSeconds)
        )
                .next()
                .map(result -> {
                    Number currentValue = (Number) result.get(0);
                    Number ttlValue = (Number) result.get(1);

                    long currentRequests = currentValue.longValue();
                    long ttlSeconds = ttlValue.longValue();

                    boolean allowed = currentRequests <= rateLimit;

                    long remainingRequests = Math.max(
                            0,
                            rateLimit - currentRequests
                    );

                    Duration retryAfter = Duration.ofSeconds(
                            Math.max(0, ttlSeconds)
                    );

                    return new RateLimitResult(
                            allowed,
                            currentRequests,
                            remainingRequests,
                            retryAfter
                    );
                });
    }
}
