package com.gateflow.gateway.apikey.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.gateflow.gateway.apikey.model.ApiUsageEvent;
import com.gateflow.gateway.apikey.repository.ApiUsageRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ApiUsageEventConsumer {

    private static final Logger log
            = LoggerFactory.getLogger(ApiUsageEventConsumer.class);

    private static final String STREAM_KEY
            = ApiUsageEventPublisher.STREAM_KEY;

    private static final String GROUP = "gateflow-usage-group";
    private static final String CONSUMER = "gateway-usage-consumer";

    private static final RedisScript<String> CREATE_GROUP_SCRIPT
            = RedisScript.of("""
                    local result = redis.pcall(
                        'XGROUP',
                        'CREATE',
                        KEYS[1],
                        ARGV[1],
                        '0',
                        'MKSTREAM'
                    )

                    if type(result) == 'table' and result.err then
                        if string.find(result.err, 'BUSYGROUP') then
                            return 'EXISTS'
                        end

                        return redis.error_reply(result.err)
                    end

                    return result
                    """, String.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiUsageRepository usageRepository;

    public ApiUsageEventConsumer(
            ReactiveStringRedisTemplate redisTemplate,
            ApiUsageRepository usageRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.usageRepository = usageRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        ensureConsumerGroup()
                .thenMany(Flux.interval(
                        Duration.ZERO,
                        Duration.ofSeconds(1)
                ).concatMap(ignored
                        -> pollPending()
                                .then(pollNew())
                                .onErrorResume(error -> {
                                    log.error(
                                            "Usage event polling failed",
                                            error
                                    );
                                    return Mono.empty();
                                })
                ))
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error(
                                "Usage consumer stopped unexpectedly",
                                error
                        )
                );
    }

    private Mono<Void> ensureConsumerGroup() {
        return redisTemplate.execute(
                CREATE_GROUP_SCRIPT,
                List.of(STREAM_KEY),
                GROUP
        ).next().then();
    }

    private Mono<Void> pollPending() {
        return read(ReadOffset.from("0-0"));
    }

    private Mono<Void> pollNew() {
        return read(ReadOffset.lastConsumed());
    }

    private Mono<Void> read(ReadOffset offset) {
        return redisTemplate.opsForStream()
                .read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(100),
                        StreamOffset.create(STREAM_KEY, offset)
                )
                .concatMap(this::persistAndAcknowledge)
                .then();
    }

    private Mono<Void> persistAndAcknowledge(
        MapRecord<String, Object, Object> record
    ) {
        try {
            Map<Object, Object> fields = record.getValue();

            Object apiIdValue = fields.get("apiId");

            ApiUsageEvent event = new ApiUsageEvent(
                    UUID.fromString(fields.get("id").toString()),
                    UUID.fromString(fields.get("apiKeyId").toString()),
                    apiIdValue == null || apiIdValue.toString().isBlank()
                            ? null
                            : UUID.fromString(apiIdValue.toString()),
                    fields.get("httpMethod").toString(),
                    fields.get("requestPath").toString(),
                    Integer.parseInt(
                            fields.get("responseStatus").toString()
                    ),
                    Long.parseLong(
                            fields.get("durationMs").toString()
                    ),
                    Instant.parse(fields.get("requestedAt").toString())
            );

            return usageRepository.save(event)
                    .then(redisTemplate.opsForStream()
                            .acknowledge(GROUP, record))
                    .then()
                    .doOnSuccess(ignored ->
                            log.debug(
                                    "Persisted usage event {}",
                                    event.id()
                            )
                    );

        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }

}
