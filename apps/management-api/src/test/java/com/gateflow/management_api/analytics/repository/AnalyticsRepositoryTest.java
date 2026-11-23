package com.gateflow.management_api.analytics.repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.dto.TrafficBucketPoint;
import com.gateflow.management_api.analytics.dto.UsageSummary;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsRepositoryTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID org1Id;
    private UUID org2Id;
    private UUID app1Id;
    private UUID app2Id;
    private UUID key1Id;
    private UUID key2Id;
    private UUID key1RevokedId;
    private UUID apiAId;
    private UUID apiBId;
    private UUID apiCId;

    @BeforeAll
    void setUpData() {
        org1Id = createOrganization("Org One", "org-one-" + UUID.randomUUID());
        org2Id = createOrganization("Org Two", "org-two-" + UUID.randomUUID());

        app1Id = createApplication(org1Id, "App One", "app-one-" + UUID.randomUUID());
        app2Id = createApplication(org2Id, "App Two", "app-two-" + UUID.randomUUID());

        key1Id = createApiKey(app1Id, "Key 1 Live", "ACTIVE");
        key1RevokedId = createApiKey(app1Id, "Key 1 Revoked", "REVOKED");
        key2Id = createApiKey(app2Id, "Key 2 Live", "ACTIVE");

        apiAId = createApi(org1Id, "Payments API", "payments-" + UUID.randomUUID(), "/v1/payments");
        apiBId = createApi(org1Id, "Users API", "users-" + UUID.randomUUID(), "/v1/users");
        apiCId = createApi(org1Id, "Invoices API", "invoices-" + UUID.randomUUID(), "/v1/invoices");
    }

    @Test
    void emptyUsageDatasetShouldReturnZeroValuesSafely() {
        UUID emptyOrgId = createOrganization("Empty Org", "empty-org-" + UUID.randomUUID());
        Instant now = Instant.now();
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        UsageSummary summary = analyticsRepository.getUsageSummary(emptyOrgId, weekAgo, now);
        assertNotNull(summary);
        assertEquals(0L, summary.totalRequests());
        assertEquals(0L, summary.successfulRequests());
        assertEquals(0L, summary.failedRequests());
        assertEquals(0.0, summary.avgLatencyMs());
        assertEquals(0.0, summary.calculateSuccessRate());

        OverviewMetricsResponse overview = analyticsRepository.getOverviewMetrics(emptyOrgId, weekAgo, now, "7d");
        assertNotNull(overview);
        assertEquals(0L, overview.totalRequests());
        assertEquals(0L, overview.successfulRequests());
        assertEquals(0L, overview.failedRequests());
        assertEquals(0.0, overview.successRate());
        assertEquals(0.0, overview.avgLatencyMs());
        assertEquals(0.0, overview.totalRequestsChangePct());
        assertEquals(0.0, overview.successRateChangePct());
        assertEquals(0.0, overview.avgLatencyChangePct());
        assertEquals(0L, overview.activeApiKeys());
        assertEquals("7d", overview.period());

        TrafficAnalyticsResponse traffic = analyticsRepository.getTrafficAnalytics(emptyOrgId, weekAgo, now, "day");
        assertNotNull(traffic);
        assertTrue(traffic.categories().isEmpty());
        assertTrue(traffic.total().isEmpty());
        assertTrue(traffic.successful().isEmpty());
        assertTrue(traffic.failed().isEmpty());

        List<TopApiResponse> topApis = analyticsRepository.getTopApis(emptyOrgId, weekAgo, now, 5);
        assertNotNull(topApis);
        assertTrue(topApis.isEmpty());
    }

    @Test
    void organizationDataIsolationShouldEnsureStrictTenancy() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant time = now.minus(30, ChronoUnit.MINUTES);

        // Seed 2 requests for Org 1
        insertUsage(key1Id, apiAId, "POST", "/v1/payments", 200, 50, time);
        insertUsage(key1Id, apiAId, "POST", "/v1/payments", 200, 60, time);

        // Seed 1 request for Org 2
        insertUsage(key2Id, null, "GET", "/v1/health", 200, 20, time);

        UsageSummary org1Summary = analyticsRepository.getUsageSummary(org1Id, oneHourAgo, now);
        assertEquals(2L, org1Summary.totalRequests());

        UsageSummary org2Summary = analyticsRepository.getUsageSummary(org2Id, oneHourAgo, now);
        assertEquals(1L, org2Summary.totalRequests());
    }

    @Test
    void successfulAndFailedRequestsAndLatencyCalculations() {
        UUID testOrgId = createOrganization("Latency Org", "latency-org-" + UUID.randomUUID());
        UUID testAppId = createApplication(testOrgId, "Latency App", "latency-app-" + UUID.randomUUID());
        UUID testKeyId = createApiKey(testAppId, "Latency Key", "ACTIVE");
        UUID testApiId = createApi(testOrgId, "Latency API", "latency-api-" + UUID.randomUUID(), "/v1/test");

        Instant now = Instant.now();
        Instant startTime = now.minus(2, ChronoUnit.HOURS);

        // 2 successful (< 400): 200 (50ms), 201 (100ms)
        insertUsage(testKeyId, testApiId, "GET", "/v1/test", 200, 50, now.minus(40, ChronoUnit.MINUTES));
        insertUsage(testKeyId, testApiId, "POST", "/v1/test", 201, 100, now.minus(30, ChronoUnit.MINUTES));

        // 2 failed (>= 400): 400 (150ms), 500 (300ms)
        insertUsage(testKeyId, testApiId, "POST", "/v1/test", 400, 150, now.minus(20, ChronoUnit.MINUTES));
        insertUsage(testKeyId, testApiId, "POST", "/v1/test", 500, 300, now.minus(10, ChronoUnit.MINUTES));

        UsageSummary summary = analyticsRepository.getUsageSummary(testOrgId, startTime, now);
        assertEquals(4L, summary.totalRequests());
        assertEquals(2L, summary.successfulRequests());
        assertEquals(2L, summary.failedRequests());
        assertEquals(50.0, summary.calculateSuccessRate());
        // (50 + 100 + 150 + 300) / 4 = 150.0 ms
        assertEquals(150.0, summary.avgLatencyMs());
    }

    @Test
    void previousPeriodComparisonsCalculation() {
        UUID testOrgId = createOrganization("Comparison Org", "comp-org-" + UUID.randomUUID());
        UUID testAppId = createApplication(testOrgId, "Comp App", "comp-app-" + UUID.randomUUID());
        UUID testKeyId = createApiKey(testAppId, "Comp Key", "ACTIVE");

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Duration window = Duration.ofDays(7);
        Instant currentStart = now.minus(window);
        Instant previousStart = currentStart.minus(window);

        // Previous period: 2 requests, 1 success, 1 fail (50% success), avg latency 200ms
        insertUsage(testKeyId, null, "GET", "/test", 200, 200, previousStart.plus(1, ChronoUnit.DAYS));
        insertUsage(testKeyId, null, "GET", "/test", 500, 200, previousStart.plus(2, ChronoUnit.DAYS));

        // Current period: 4 requests, 3 success, 1 fail (75% success), avg latency 100ms
        insertUsage(testKeyId, null, "GET", "/test", 200, 100, currentStart.plus(1, ChronoUnit.DAYS));
        insertUsage(testKeyId, null, "GET", "/test", 200, 100, currentStart.plus(2, ChronoUnit.DAYS));
        insertUsage(testKeyId, null, "GET", "/test", 200, 100, currentStart.plus(3, ChronoUnit.DAYS));
        insertUsage(testKeyId, null, "GET", "/test", 500, 100, currentStart.plus(4, ChronoUnit.DAYS));

        OverviewMetricsResponse response = analyticsRepository.getOverviewMetrics(testOrgId, currentStart, now, "7d");
        assertEquals(4L, response.totalRequests());
        assertEquals(3L, response.successfulRequests());
        assertEquals(1L, response.failedRequests());
        assertEquals(75.0, response.successRate());
        assertEquals(100.0, response.avgLatencyMs());

        // totalRequestsChangePct: (4 - 2) / 2 * 100 = +100.0%
        assertEquals(100.0, response.totalRequestsChangePct());
        // successRateChangePct: 75.0 - 50.0 = +25.0 percentage points
        assertEquals(25.0, response.successRateChangePct());
        // avgLatencyChangePct: (100 - 200) / 200 * 100 = -50.0%
        assertEquals(-50.0, response.avgLatencyChangePct());
    }

    @Test
    void trafficAggregationByTimeBucket() {
        UUID testOrgId = createOrganization("Traffic Org", "traffic-org-" + UUID.randomUUID());
        UUID testAppId = createApplication(testOrgId, "Traffic App", "traffic-app-" + UUID.randomUUID());
        UUID testKeyId = createApiKey(testAppId, "Traffic Key", "ACTIVE");

        Instant baseTime = Instant.parse("2026-11-20T00:00:00Z");
        Instant day1 = baseTime.plus(1, ChronoUnit.DAYS);
        Instant day2 = baseTime.plus(2, ChronoUnit.DAYS);
        Instant end = baseTime.plus(3, ChronoUnit.DAYS);

        // Day 1: 2 success, 1 fail
        insertUsage(testKeyId, null, "GET", "/test", 200, 50, day1.plus(2, ChronoUnit.HOURS));
        insertUsage(testKeyId, null, "GET", "/test", 200, 50, day1.plus(4, ChronoUnit.HOURS));
        insertUsage(testKeyId, null, "GET", "/test", 500, 80, day1.plus(6, ChronoUnit.HOURS));

        // Day 2: 1 success, 0 fail
        insertUsage(testKeyId, null, "GET", "/test", 200, 40, day2.plus(1, ChronoUnit.HOURS));

        List<TrafficBucketPoint> buckets = analyticsRepository.getTrafficBuckets(testOrgId, baseTime, end, "day");
        assertEquals(2, buckets.size());

        TrafficBucketPoint b1 = buckets.get(0);
        assertEquals(3L, b1.total());
        assertEquals(2L, b1.successful());
        assertEquals(1L, b1.failed());

        TrafficBucketPoint b2 = buckets.get(1);
        assertEquals(1L, b2.total());
        assertEquals(1L, b2.successful());
        assertEquals(0L, b2.failed());

        TrafficAnalyticsResponse response = analyticsRepository.getTrafficAnalytics(testOrgId, baseTime, end, "day");
        assertEquals(2, response.categories().size());
        assertEquals(List.of(3L, 1L), response.total());
        assertEquals(List.of(2L, 1L), response.successful());
        assertEquals(List.of(1L, 0L), response.failed());
    }

    @Test
    void topApiOrderingAndLimit() {
        UUID testOrgId = createOrganization("Top API Org", "top-api-org-" + UUID.randomUUID());
        UUID testAppId = createApplication(testOrgId, "Top App", "top-app-" + UUID.randomUUID());
        UUID testKeyId = createApiKey(testAppId, "Top Key", "ACTIVE");

        UUID api1 = createApi(testOrgId, "Low Volume API", "api-low-" + UUID.randomUUID(), "/v1/low");
        UUID api2 = createApi(testOrgId, "High Volume API", "api-high-" + UUID.randomUUID(), "/v1/high");
        UUID api3 = createApi(testOrgId, "Medium Volume API", "api-med-" + UUID.randomUUID(), "/v1/med");

        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.DAYS);

        // 1 request to api1
        insertUsage(testKeyId, api1, "GET", "/v1/low", 200, 50, now.minus(30, ChronoUnit.MINUTES));

        // 3 requests to api2
        insertUsage(testKeyId, api2, "POST", "/v1/high", 200, 80, now.minus(25, ChronoUnit.MINUTES));
        insertUsage(testKeyId, api2, "POST", "/v1/high", 200, 70, now.minus(20, ChronoUnit.MINUTES));
        insertUsage(testKeyId, api2, "POST", "/v1/high", 500, 90, now.minus(15, ChronoUnit.MINUTES));

        // 2 requests to api3
        insertUsage(testKeyId, api3, "GET", "/v1/med", 200, 30, now.minus(10, ChronoUnit.MINUTES));
        insertUsage(testKeyId, api3, "GET", "/v1/med", 200, 40, now.minus(5, ChronoUnit.MINUTES));

        // Query top 2 APIs
        List<TopApiResponse> topApis = analyticsRepository.getTopApis(testOrgId, start, now, 2);
        assertEquals(2, topApis.size());

        // 1st should be api2 (3 requests)
        assertEquals(api2, topApis.get(0).id());
        assertEquals("High Volume API", topApis.get(0).name());
        assertEquals(3L, topApis.get(0).requests());
        assertEquals(66.67, topApis.get(0).successRate());

        // 2nd should be api3 (2 requests)
        assertEquals(api3, topApis.get(1).id());
        assertEquals("Medium Volume API", topApis.get(1).name());
        assertEquals(2L, topApis.get(1).requests());
        assertEquals(100.0, topApis.get(1).successRate());
    }

    @Test
    void activeApiKeyCount() {
        UUID testOrgId = createOrganization("Keys Org", "keys-org-" + UUID.randomUUID());
        UUID testAppId = createApplication(testOrgId, "Keys App", "keys-app-" + UUID.randomUUID());

        // 2 ACTIVE keys, 1 REVOKED key
        createApiKey(testAppId, "Key Active 1", "ACTIVE");
        createApiKey(testAppId, "Key Active 2", "ACTIVE");
        createApiKey(testAppId, "Key Revoked 1", "REVOKED");

        long activeCount = analyticsRepository.countActiveApiKeys(testOrgId);
        assertEquals(2L, activeCount);
    }

    // --- Helper methods to insert test fixture data ---

    private UUID createOrganization(String name, String slug) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO organizations (id, name, slug, status)
                VALUES (?, ?, ?, 'ACTIVE')
                """, id, name, slug);
        return id;
    }

    private UUID createApplication(UUID organizationId, String name, String slug) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO applications (id, organization_id, name, slug, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """, id, organizationId, name, slug);
        return id;
    }

    private UUID createApiKey(UUID applicationId, String name, String status) {
        UUID id = UUID.randomUUID();
        String prefix = "key_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String hash = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Timestamp revokedAt = "REVOKED".equals(status) ? Timestamp.from(Instant.now()) : null;

        jdbcTemplate.update("""
                INSERT INTO application_api_keys (id, application_id, name, environment, key_prefix, key_hash, status, revoked_at)
                VALUES (?, ?, ?, 'LIVE', ?, ?, ?, ?)
                """, id, applicationId, name, prefix, hash, status, revokedAt);
        return id;
    }

    private UUID createApi(UUID organizationId, String name, String slug, String baseUrl) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO apis (id, organization_id, name, slug, base_url, version, status)
                VALUES (?, ?, ?, ?, ?, 'v1', 'ACTIVE')
                """, id, organizationId, name, slug, baseUrl);
        return id;
    }

    private void insertUsage(UUID apiKeyId, UUID apiId, String method, String path, int status, long durationMs, Instant requestedAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO api_usage (id, api_key_id, api_id, http_method, request_path, response_status, duration_ms, requested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, apiKeyId, apiId, method, path, status, durationMs, Timestamp.from(requestedAt));
    }
}
