package com.gateflow.gateway.apikey.repository;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.gateflow.gateway.apikey.model.ApiRouteRecord;

import reactor.core.publisher.Mono;

@Repository
public class ApiRouteRepository {

    private final DatabaseClient databaseClient;

    public ApiRouteRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<ApiRouteRecord> findActiveSubscribedApi(
            UUID applicationId,
            String slug,
            String version
    ) {
        return databaseClient.sql("""
                SELECT
                    a.id AS api_id,
                    a.organization_id,
                    a.slug,
                    a.version,
                    a.base_url
                FROM subscriptions s
                INNER JOIN apis a
                    ON a.id = s.api_id
                   AND a.organization_id = s.organization_id
                WHERE s.application_id = :applicationId
                  AND s.status = 'ACTIVE'
                  AND a.slug = :slug
                  AND a.version = :version
                  AND a.status = 'ACTIVE'
                """)
                .bind("applicationId", applicationId)
                .bind("slug", slug)
                .bind("version", version)
                .map((row, metadata) -> new ApiRouteRecord(
                        row.get("api_id", UUID.class),
                        row.get("organization_id", UUID.class),
                        row.get("slug", String.class),
                        row.get("version", String.class),
                        row.get("base_url", String.class)
                ))
                .one();
    }
}
