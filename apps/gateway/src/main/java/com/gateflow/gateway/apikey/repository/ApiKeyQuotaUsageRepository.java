package com.gateflow.gateway.apikey.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public class ApiKeyQuotaUsageRepository {

    private final DatabaseClient databaseClient;

    public ApiKeyQuotaUsageRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Long> reserve(
            UUID apiKeyId,
            String quotaWindow,
            Instant windowStart,
            long quotaLimit
    ) {
        String sql = """
            INSERT INTO api_key_quota_usage (
                api_key_id,
                quota_window,
                window_start,
                request_count
            )
            VALUES (
                :apiKeyId,
                :quotaWindow,
                :windowStart,
                1
            )
            ON CONFLICT (api_key_id, quota_window, window_start)
            DO UPDATE SET
                request_count = api_key_quota_usage.request_count + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE api_key_quota_usage.request_count < :quotaLimit
            RETURNING request_count
            """;

        return databaseClient.sql(sql)
                .bind("apiKeyId", apiKeyId)
                .bind("quotaWindow", quotaWindow)
                .bind("windowStart", windowStart)
                .bind("quotaLimit", quotaLimit)
                .fetch()
                .one()
                .map(row -> ((Number) row.get("request_count")).longValue());
    }
}
