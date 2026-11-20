package com.gateflow.gateway.apikey.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;

import reactor.core.publisher.Mono;

@Repository
public class ApiKeyRepository {

    private final DatabaseClient databaseClient;

    public ApiKeyRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<ApiKeyRecord> findValidApiKey(String keyPrefix, String keyHash) {
        return databaseClient.sql("""
                SELECT
                    api_key.id,
                    api_key.application_id,
                    api_key.key_prefix,
                    api_key.key_hash,
                    api_key.environment,
                    api_key.status,
                    api_key.expires_at
                FROM application_api_keys api_key
                INNER JOIN applications app
                    ON app.id = api_key.application_id
                INNER JOIN organizations org
                    ON org.id = app.organization_id
                WHERE api_key.key_prefix = :keyPrefix
                  AND api_key.key_hash = :keyHash
                  AND api_key.status = 'ACTIVE'
                  AND (api_key.expires_at IS NULL OR api_key.expires_at > :now)
                  AND app.status = 'ACTIVE'
                  AND org.status = 'ACTIVE'
                """)
                .bind("keyPrefix", keyPrefix)
                .bind("keyHash", keyHash)
                .bind("now", Instant.now())
                .map((row, metadata) -> new ApiKeyRecord(
                        row.get("id", UUID.class),
                        row.get("application_id", UUID.class),
                        row.get("key_prefix", String.class),
                        row.get("key_hash", String.class),
                        row.get("environment", String.class),
                        row.get("status", String.class),
                        row.get("expires_at", Instant.class)
                ))
                .one();
    }
}
