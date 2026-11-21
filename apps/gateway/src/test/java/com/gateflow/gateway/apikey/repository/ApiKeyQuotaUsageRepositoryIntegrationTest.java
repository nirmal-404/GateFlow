package com.gateflow.gateway.apikey.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.r2dbc.core.DatabaseClient;

import reactor.core.publisher.Flux;

@Testcontainers
class ApiKeyQuotaUsageRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres
            = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gateflow_test")
                    .withUsername("gateflow")
                    .withPassword("gateflow_test_password");

    private static DatabaseClient databaseClient;

    private ApiKeyQuotaUsageRepository repository;

    private UUID apiKeyId;

    private Instant windowStart;

    @BeforeAll
    static void initializeDatabaseClient() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, postgres.getHost())
                .option(ConnectionFactoryOptions.PORT, postgres.getFirstMappedPort())
                .option(ConnectionFactoryOptions.USER, postgres.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, postgres.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, postgres.getDatabaseName())
                .build();

        ConnectionFactory connectionFactory
                = ConnectionFactories.get(options);

        databaseClient = DatabaseClient.create(connectionFactory);
    }

    @BeforeEach
    void setUp() {
        databaseClient.sql("""
                DROP TABLE IF EXISTS api_key_quota_usage;
                """).then().block();

        databaseClient.sql("""
                DROP TABLE IF EXISTS application_api_keys;
                """).then().block();

        databaseClient.sql("""
                CREATE TABLE application_api_keys (
                    id UUID PRIMARY KEY
                )
                """).then().block();

        databaseClient.sql("""
                CREATE TABLE api_key_quota_usage (
                    api_key_id UUID NOT NULL,
                    quota_window VARCHAR(20) NOT NULL,
                    window_start TIMESTAMPTZ NOT NULL,
                    request_count BIGINT DEFAULT 0 NOT NULL,
                    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    CONSTRAINT pk_api_key_quota_usage
                        PRIMARY KEY (api_key_id, quota_window, window_start),
                    CONSTRAINT fk_api_key_quota_usage_api_key
                        FOREIGN KEY (api_key_id)
                        REFERENCES application_api_keys (id),
                    CONSTRAINT ck_api_key_quota_usage_window
                        CHECK (quota_window IN ('DAILY', 'WEEKLY', 'MONTHLY')),
                    CONSTRAINT ck_api_key_quota_usage_count
                        CHECK (request_count >= 0)
                )
                """).then().block();

        repository = new ApiKeyQuotaUsageRepository(databaseClient);

        apiKeyId = UUID.randomUUID();
        windowStart = Instant.parse("2026-11-27T00:00:00Z");

        databaseClient.sql("""
                INSERT INTO application_api_keys (id)
                VALUES (:apiKeyId)
                """)
                .bind("apiKeyId", apiKeyId)
                .then()
                .block();
    }

    @Test
    void shouldRejectRequestsAfterQuotaLimitIsReached() {
        Long first = repository.reserve(
                apiKeyId, "DAILY", windowStart, 3
        ).block();

        Long second = repository.reserve(
                apiKeyId, "DAILY", windowStart, 3
        ).block();

        Long third = repository.reserve(
                apiKeyId, "DAILY", windowStart, 3
        ).block();

        Long fourth = repository.reserve(
                apiKeyId, "DAILY", windowStart, 3
        ).block();

        assertEquals(1L, first);
        assertEquals(2L, second);
        assertEquals(3L, third);
        assertEquals(null, fourth);

        assertEquals(3L, readRequestCount());
    }

    @Test
    void shouldEnforceQuotaUnderConcurrentRequests() {
        int totalRequests = 50;
        long quotaLimit = 10;

        List<Boolean> results = Flux.range(0, totalRequests)
                .flatMap(index
                        -> repository.reserve(
                                apiKeyId,
                                "DAILY",
                                windowStart,
                                quotaLimit
                        )
                                .map(count -> true)
                                .defaultIfEmpty(false),
                        totalRequests
                )
                .collectList()
                .block();

        assertNotNull(results);

        long allowedRequests = results.stream()
                .filter(Boolean::booleanValue)
                .count();

        assertEquals(quotaLimit, allowedRequests);
        assertEquals(quotaLimit, readRequestCount());
    }

    @Test
    void shouldMaintainIndependentQuotaWindows() {
        Long dailyCount = repository.reserve(
                apiKeyId,
                "DAILY",
                windowStart,
                1
        ).block();

        Long monthlyCount = repository.reserve(
                apiKeyId,
                "MONTHLY",
                windowStart,
                1
        ).block();

        assertEquals(1L, dailyCount);
        assertEquals(1L, monthlyCount);
    }

    private long readRequestCount() {
        return databaseClient.sql("""
                SELECT request_count
                FROM api_key_quota_usage
                WHERE api_key_id = :apiKeyId
                  AND quota_window = 'DAILY'
                  AND window_start = :windowStart
                """)
                .bind("apiKeyId", apiKeyId)
                .bind("windowStart", windowStart)
                .map(row -> ((Number) row.get("request_count")).longValue())
                .one()
                .block();
    }
}
