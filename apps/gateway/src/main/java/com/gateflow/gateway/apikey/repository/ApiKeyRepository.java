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
                    id,
                    application_id,
                    key_prefix,
                    key_hash,
                    environment,
                    status,
                    expires_at
                FROM application_api_keys
                WHERE key_prefix = :keyPrefix
                  AND key_hash = :keyHash
                  AND status = 'ACTIVE'
                  AND (expires_at IS NULL OR expires_at > :now)
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
