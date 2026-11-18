package com.gateflow.gateway.apikey.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.ApiUsageEvent;

import reactor.core.publisher.Mono;

@Service
public class ApiUsageEventPublisher {

    public static final String STREAM_KEY = "gateflow:usage-events";

    private final ReactiveStringRedisTemplate redisTemplate;

    public ApiUsageEventPublisher(
            ReactiveStringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Void> publish(ApiUsageEvent event) {
        Map<String, String> fields = new HashMap<>();

        fields.put("id", event.id().toString());
        fields.put("apiKeyId", event.apiKeyId().toString());
        fields.put(
                "apiId",
                event.apiId() == null ? "" : event.apiId().toString()
        );
        fields.put("httpMethod", event.httpMethod());
        fields.put("requestPath", event.requestPath());
        fields.put("responseStatus", String.valueOf(event.responseStatus()));
        fields.put("durationMs", String.valueOf(event.durationMs()));
        fields.put("requestedAt", event.requestedAt().toString());

        MapRecord<String, String, String> record
                = MapRecord.create(STREAM_KEY, fields);

        return redisTemplate.opsForStream()
                .add(record)
                .then();
    }
}
