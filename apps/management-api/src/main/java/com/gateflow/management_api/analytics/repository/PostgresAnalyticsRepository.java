package com.gateflow.management_api.analytics.repository;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.dto.TrafficBucketPoint;
import com.gateflow.management_api.analytics.dto.UsageSummary;

@Repository
@Transactional(readOnly = true)
public class PostgresAnalyticsRepository implements AnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UsageSummary getUsageSummary(UUID organizationId, Instant startTime, Instant endTime) {
        String sql = """
                SELECT
                    COUNT(*) AS total_requests,
                    COUNT(CASE WHEN u.response_status < 400 THEN 1 END) AS successful_requests,
                    COUNT(CASE WHEN u.response_status >= 400 THEN 1 END) AS failed_requests,
                    COALESCE(AVG(u.duration_ms), 0.0) AS avg_latency_ms
                FROM api_usage u
                JOIN application_api_keys k ON u.api_key_id = k.id
                JOIN applications a ON k.application_id = a.id
                WHERE a.organization_id = :organizationId
                  AND u.requested_at >= :startTime
                  AND u.requested_at < :endTime
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("startTime", Timestamp.from(startTime))
                .addValue("endTime", Timestamp.from(endTime));

        List<UsageSummary> results = jdbcTemplate.query(sql, params, (rs, rowNum) -> mapUsageSummary(rs));

        if (results.isEmpty()) {
            return UsageSummary.empty();
        }
        return results.getFirst();
    }

    @Override
    public OverviewMetricsResponse getOverviewMetrics(UUID organizationId, Instant startTime, Instant endTime, String period) {
        Duration duration = Duration.between(startTime, endTime);
        Instant prevStartTime = startTime.minus(duration);
        Instant prevEndTime = startTime;

        UsageSummary current = getUsageSummary(organizationId, startTime, endTime);
        UsageSummary previous = getUsageSummary(organizationId, prevStartTime, prevEndTime);
        long activeKeys = countActiveApiKeys(organizationId);

        double currentSuccessRate = current.calculateSuccessRate();
        double prevSuccessRate = previous.calculateSuccessRate();

        Double totalRequestsChangePct = calculateTotalRequestsChange(current.totalRequests(), previous.totalRequests());
        Double successRateChangePct = calculateSuccessRateChange(currentSuccessRate, prevSuccessRate, current.totalRequests(), previous.totalRequests());
        Double avgLatencyChangePct = calculateLatencyChange(current.avgLatencyMs(), previous.avgLatencyMs(), current.totalRequests(), previous.totalRequests());

        return new OverviewMetricsResponse(
                current.totalRequests(),
                current.successfulRequests(),
                current.failedRequests(),
                currentSuccessRate,
                roundToOneDecimal(current.avgLatencyMs()),
                totalRequestsChangePct,
                successRateChangePct,
                avgLatencyChangePct,
                activeKeys,
                period
        );
    }

    @Override
    public List<TrafficBucketPoint> getTrafficBuckets(UUID organizationId, Instant startTime, Instant endTime, String bucketUnit) {
        String safeBucketUnit = sanitizeBucketUnit(bucketUnit);

        String sql = String.format("""
                SELECT
                    date_trunc('%s', u.requested_at) AS bucket_time,
                    COUNT(*) AS total_requests,
                    COUNT(CASE WHEN u.response_status < 400 THEN 1 END) AS successful_requests,
                    COUNT(CASE WHEN u.response_status >= 400 THEN 1 END) AS failed_requests
                FROM api_usage u
                JOIN application_api_keys k ON u.api_key_id = k.id
                JOIN applications a ON k.application_id = a.id
                WHERE a.organization_id = :organizationId
                  AND u.requested_at >= :startTime
                  AND u.requested_at < :endTime
                GROUP BY bucket_time
                ORDER BY bucket_time ASC
                """, safeBucketUnit);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("startTime", Timestamp.from(startTime))
                .addValue("endTime", Timestamp.from(endTime));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("bucket_time");
            Instant instant = ts != null ? ts.toInstant() : Instant.EPOCH;
            return new TrafficBucketPoint(
                    instant,
                    rs.getLong("total_requests"),
                    rs.getLong("successful_requests"),
                    rs.getLong("failed_requests")
            );
        });
    }

    @Override
    public TrafficAnalyticsResponse getTrafficAnalytics(UUID organizationId, Instant startTime, Instant endTime, String bucketUnit) {
        List<TrafficBucketPoint> points = getTrafficBuckets(organizationId, startTime, endTime, bucketUnit);

        String normalizedUnit = bucketUnit != null ? bucketUnit.trim().toLowerCase(Locale.ROOT) : "day";
        DateTimeFormatter formatter = "hour".equals(normalizedUnit)
                ? DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC)
                : DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

        List<String> categories = new ArrayList<>(points.size());
        List<Long> total = new ArrayList<>(points.size());
        List<Long> successful = new ArrayList<>(points.size());
        List<Long> failed = new ArrayList<>(points.size());

        for (TrafficBucketPoint point : points) {
            categories.add(formatter.format(point.bucketTime()));
            total.add(point.total());
            successful.add(point.successful());
            failed.add(point.failed());
        }

        return new TrafficAnalyticsResponse(categories, total, successful, failed);
    }

    @Override
    public List<TopApiResponse> getTopApis(UUID organizationId, Instant startTime, Instant endTime, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);

        String sql = """
                SELECT
                    api.id AS api_id,
                    COALESCE(api.name, u.request_path) AS api_name,
                    u.request_path AS api_path,
                    u.http_method AS http_method,
                    COUNT(*) AS total_requests,
                    COUNT(CASE WHEN u.response_status < 400 THEN 1 END) AS successful_requests,
                    COALESCE(AVG(u.duration_ms), 0.0) AS avg_latency_ms
                FROM api_usage u
                JOIN application_api_keys k ON u.api_key_id = k.id
                JOIN applications a ON k.application_id = a.id
                LEFT JOIN apis api ON u.api_id = api.id
                WHERE a.organization_id = :organizationId
                  AND u.requested_at >= :startTime
                  AND u.requested_at < :endTime
                GROUP BY api.id, api.name, u.request_path, u.http_method
                ORDER BY total_requests DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("startTime", Timestamp.from(startTime))
                .addValue("endTime", Timestamp.from(endTime))
                .addValue("limit", safeLimit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            UUID apiId = rs.getObject("api_id", UUID.class);
            String name = rs.getString("api_name");
            String path = rs.getString("api_path");
            String method = rs.getString("http_method");
            long requests = rs.getLong("total_requests");
            long successful = rs.getLong("successful_requests");
            double avgLatency = rs.getDouble("avg_latency_ms");

            if (apiId == null) {
                // Ensure a stable, non-null ID if the endpoint had no assigned API entity
                apiId = UUID.nameUUIDFromBytes((method + ":" + path).getBytes(StandardCharsets.UTF_8));
            }

            double successRate = requests == 0 ? 0.0 : roundToTwoDecimals(((double) successful / requests) * 100.0);

            return new TopApiResponse(
                    apiId,
                    name,
                    path,
                    method,
                    requests,
                    successRate,
                    roundToOneDecimal(avgLatency)
            );
        });
    }

    @Override
    public long countActiveApiKeys(UUID organizationId) {
        String sql = """
                SELECT COUNT(k.id)
                FROM application_api_keys k
                JOIN applications a ON k.application_id = a.id
                WHERE a.organization_id = :organizationId
                  AND k.status = 'ACTIVE'
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId);

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return Objects.requireNonNullElse(count, 0L);
    }

    private UsageSummary mapUsageSummary(ResultSet rs) throws SQLException {
        long total = rs.getLong("total_requests");
        long successful = rs.getLong("successful_requests");
        long failed = rs.getLong("failed_requests");
        double avgLatency = rs.getDouble("avg_latency_ms");
        return new UsageSummary(total, successful, failed, roundToOneDecimal(avgLatency));
    }

    private String sanitizeBucketUnit(String bucketUnit) {
        if (bucketUnit == null) {
            return "day";
        }
        return switch (bucketUnit.trim().toLowerCase(Locale.ROOT)) {
            case "hour" -> "hour";
            case "week" -> "week";
            case "month" -> "month";
            default -> "day";
        };
    }

    private Double calculateTotalRequestsChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        return roundToOneDecimal(((double) (current - previous) / previous) * 100.0);
    }

    private Double calculateSuccessRateChange(double currentRate, double prevRate, long currentTotal, long prevTotal) {
        if (prevTotal == 0 && currentTotal == 0) {
            return 0.0;
        }
        // Percentage point difference (e.g. 99.8% vs 99.4% = +0.4%)
        return roundToOneDecimal(currentRate - prevRate);
    }

    private Double calculateLatencyChange(double currentLatency, double prevLatency, long currentTotal, long prevTotal) {
        if (prevTotal == 0 || prevLatency == 0.0) {
            return (currentTotal == 0 || currentLatency == 0.0) ? 0.0 : 100.0;
        }
        return roundToOneDecimal(((currentLatency - prevLatency) / prevLatency) * 100.0);
    }

    private double roundToOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private double roundToTwoDecimals(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
