package com.gateflow.gateway.apikey.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.test.StepVerifier;

@SpringBootTest
@Testcontainers
class ApiKeyRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES
            = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gateflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", ()
                -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getFirstMappedPort() + "/"
                + POSTGRES.getDatabaseName());

        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private UUID organizationId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        createSchema();
        clearTestData();

        organizationId = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        databaseClient.sql("""
                INSERT INTO organizations (id, name, slug, status)
                VALUES (:id, 'Test Organization', :slug, 'ACTIVE')
                """)
                .bind("id", organizationId)
                .bind("slug", "org-" + organizationId)
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("""
                INSERT INTO applications (
                    id, organization_id, name, slug, status
                )
                VALUES (
                    :id, :organizationId, 'Test Application',
                    :slug, 'ACTIVE'
                )
                """)
                .bind("id", applicationId)
                .bind("organizationId", organizationId)
                .bind("slug", "app-" + applicationId)
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Test
    void shouldFindValidActiveApiKey() {
        insertApiKey("active-key", "ACTIVE", null);

        StepVerifier.create(
                apiKeyRepository.findValidApiKey(
                        "0123456789abcdef",
                        "active-key"
                )
        )
                .assertNext(key -> assertEquals("ACTIVE", key.status()))
                .verifyComplete();
    }

    @Test
    void shouldRejectRevokedApiKey() {
        insertApiKey("revoked-key", "REVOKED", Instant.now());

        assertKeyNotFound("revoked-key");
    }

    @Test
    void shouldRejectExpiredApiKey() {
        insertApiKey(
                "expired-key",
                "ACTIVE",
                null,
                Instant.now().minusSeconds(60)
        );

        assertKeyNotFound("expired-key");
    }

    @Test
    void shouldRejectApiKeyForInactiveApplication() {
        insertApiKey("inactive-app-key", "ACTIVE", null);

        updateStatus("applications", applicationId, "INACTIVE");

        assertKeyNotFound("inactive-app-key");
    }

    @Test
    void shouldRejectApiKeyForSuspendedOrganization() {
        insertApiKey("suspended-org-key", "ACTIVE", null);

        updateStatus("organizations", organizationId, "SUSPENDED");

        assertKeyNotFound("suspended-org-key");
    }

    private void clearTestData() {
        databaseClient.sql("DELETE FROM application_api_keys")
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("DELETE FROM applications")
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("DELETE FROM organizations")
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void assertKeyNotFound(String keyHash) {
        StepVerifier.create(
                apiKeyRepository.findValidApiKey(
                        "0123456789abcdef",
                        keyHash
                )
        )
                .verifyComplete();
    }

    private void insertApiKey(
            String keyHash,
            String status,
            Instant revokedAt
    ) {
        insertApiKey(keyHash, status, revokedAt, null);
    }

    private void insertApiKey(
            String keyHash,
            String status,
            Instant revokedAt,
            Instant expiresAt
    ) {
        var query = databaseClient.sql("""
                INSERT INTO application_api_keys (
                    id, application_id, name, environment,
                    key_prefix, key_hash, status, revoked_at, expires_at
                )
                VALUES (
                    :id, :applicationId, 'Test Key', 'LIVE',
                    :prefix, :keyHash, :status, :revokedAt, :expiresAt
                )
                """)
                .bind("id", UUID.randomUUID())
                .bind("applicationId", applicationId)
                .bind("prefix", "0123456789abcdef")
                .bind("keyHash", keyHash)
                .bind("status", status);

        query = revokedAt == null
                ? query.bindNull("revokedAt", Instant.class)
                : query.bind("revokedAt", revokedAt);

        query = expiresAt == null
                ? query.bindNull("expiresAt", Instant.class)
                : query.bind("expiresAt", expiresAt);

        query.fetch().rowsUpdated().block();
    }

    private void updateStatus(
            String table,
            UUID id,
            String status
    ) {
        String sql = table.equals("applications")
                ? "UPDATE applications SET status = :status WHERE id = :id"
                : "UPDATE organizations SET status = :status WHERE id = :id";

        databaseClient.sql(sql)
                .bind("status", status)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void createSchema() {
        databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS organizations (
                    id UUID PRIMARY KEY,
                    name VARCHAR(120) NOT NULL,
                    slug VARCHAR(80) NOT NULL UNIQUE,
                    status VARCHAR(20) NOT NULL
                )
                """)
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS applications (
                    id UUID PRIMARY KEY,
                    organization_id UUID NOT NULL REFERENCES organizations(id),
                    name VARCHAR(120) NOT NULL,
                    slug VARCHAR(80) NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """)
                .fetch()
                .rowsUpdated()
                .block();

        databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS application_api_keys (
                    id UUID PRIMARY KEY,
                    application_id UUID NOT NULL REFERENCES applications(id),
                    name VARCHAR(120) NOT NULL,
                    environment VARCHAR(10) NOT NULL,
                    key_prefix VARCHAR(16) NOT NULL UNIQUE,
                    key_hash VARCHAR(64) NOT NULL UNIQUE,
                    status VARCHAR(20) NOT NULL,
                    revoked_at TIMESTAMPTZ,
                    expires_at TIMESTAMPTZ,
                    CHECK (
                        (status = 'ACTIVE' AND revoked_at IS NULL)
                        OR
                        (status = 'REVOKED' AND revoked_at IS NOT NULL)
                    )
                )
                """)
                .fetch()
                .rowsUpdated()
                .block();
    }
}
