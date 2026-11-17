
package com.gateflow.gateway.apikey.repository;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;

import reactor.core.publisher.Mono;

@Repository
public class ApiKeyPolicyRepository {

    private final DatabaseClient databaseClient;

    public ApiKeyPolicyRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<ApiKeyPolicyRecord> findByApiKeyId(UUID apiKeyId) {
        return databaseClient.sql("""
                SELECT
                    id,
                    api_key_id,
                    rate_limit,
                    rate_limit_window_seconds,
                    quota_limit,
                    quota_window,
                    policy_source
                FROM api_key_policies
                WHERE api_key_id = :apiKeyId
                """)
                .bind("apiKeyId", apiKeyId)
                .map((row, metadata) -> new ApiKeyPolicyRecord(
                        row.get("id", UUID.class),
                        row.get("api_key_id", UUID.class),
                        row.get("rate_limit", Integer.class),
                        row.get("rate_limit_window_seconds", Integer.class),
                        row.get("quota_limit", Long.class),
                        row.get("quota_window", String.class),
                        row.get("policy_source", String.class)
                ))
                .one();
    }
}
