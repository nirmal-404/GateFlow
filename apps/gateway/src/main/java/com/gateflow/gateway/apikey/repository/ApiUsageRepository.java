package com.gateflow.gateway.apikey.repository;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.gateflow.gateway.apikey.model.ApiUsageEvent;

import reactor.core.publisher.Mono;

@Repository
public class ApiUsageRepository {

    private final DatabaseClient databaseClient;

    public ApiUsageRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> save(ApiUsageEvent event) {
        DatabaseClient.GenericExecuteSpec query = databaseClient.sql("""
                INSERT INTO api_usage (
                    id,
                    api_key_id,
                    api_id,
                    http_method,
                    request_path,
                    response_status,
                    duration_ms,
                    requested_at
                )
                VALUES (
                    :id,
                    :apiKeyId,
                    :apiId,
                    :httpMethod,
                    :requestPath,
                    :responseStatus,
                    :durationMs,
                    :requestedAt
                )
                ON CONFLICT (id) DO NOTHING
                """)
                .bind("id", event.id())
                .bind("apiKeyId", event.apiKeyId())
                .bind("httpMethod", event.httpMethod())
                .bind("requestPath", event.requestPath())
                .bind("responseStatus", event.responseStatus())
                .bind("durationMs", event.durationMs())
                .bind("requestedAt", event.requestedAt());

        if (event.apiId() == null) {
            query = query.bindNull("apiId", java.util.UUID.class);
        } else {
            query = query.bind("apiId", event.apiId());
        }

        return query.fetch().rowsUpdated().then();
    }
}
