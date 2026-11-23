package com.gateflow.management_api.analytics.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.repository.AnalyticsRepository;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final List<String> SUPPORTED_RANGES = List.of("24h", "7d", "30d");
    private static final List<String> SUPPORTED_INTERVALS = List.of("hour", "day", "week");

    private final AnalyticsRepository analyticsRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            OrganizationAuthorizationService authorizationService
    ) {
        this(analyticsRepository, authorizationService, Clock.systemUTC());
    }

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            OrganizationAuthorizationService authorizationService,
            Clock clock
    ) {
        this.analyticsRepository = analyticsRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    public OverviewMetricsResponse getOverviewMetrics(UUID organizationId, String range) {
        authorizationService.requirePermission(
                organizationId,
                OrganizationPermission.VIEW_ORGANIZATION
        );

        String normalizedRange = validateAndNormalizeRange(range);
        TimeWindow window = calculateTimeWindow(normalizedRange);

        return analyticsRepository.getOverviewMetrics(
                organizationId,
                window.startTime(),
                window.endTime(),
                normalizedRange
        );
    }

    public TrafficAnalyticsResponse getTrafficAnalytics(UUID organizationId, String range, String interval) {
        authorizationService.requirePermission(
                organizationId,
                OrganizationPermission.VIEW_ORGANIZATION
        );

        String normalizedRange = validateAndNormalizeRange(range);
        String normalizedInterval = resolveAndValidateInterval(interval, normalizedRange);
        TimeWindow window = calculateTimeWindow(normalizedRange);

        return analyticsRepository.getTrafficAnalytics(
                organizationId,
                window.startTime(),
                window.endTime(),
                normalizedInterval
        );
    }

    public List<TopApiResponse> getTopApis(UUID organizationId, String range, int limit) {
        authorizationService.requirePermission(
                organizationId,
                OrganizationPermission.VIEW_ORGANIZATION
        );

        String normalizedRange = validateAndNormalizeRange(range);
        validateLimit(limit);
        TimeWindow window = calculateTimeWindow(normalizedRange);

        return analyticsRepository.getTopApis(
                organizationId,
                window.startTime(),
                window.endTime(),
                limit
        );
    }

    private String validateAndNormalizeRange(String range) {
        if (range == null || range.isBlank()) {
            return "7d";
        }
        String normalized = range.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_RANGES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid range: '" + range + "'. Supported ranges are: " + String.join(", ", SUPPORTED_RANGES)
            );
        }
        return normalized;
    }

    private String resolveAndValidateInterval(String interval, String normalizedRange) {
        if (interval == null || interval.isBlank()) {
            return switch (normalizedRange) {
                case "24h" -> "hour";
                case "30d" -> "day";
                default -> "day";
            };
        }
        String normalized = interval.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_INTERVALS.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid interval: '" + interval + "'. Supported intervals are: " + String.join(", ", SUPPORTED_INTERVALS)
            );
        }
        return normalized;
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
    }

    private TimeWindow calculateTimeWindow(String normalizedRange) {
        Instant now = Instant.now(clock);
        Instant startTime = switch (normalizedRange) {
            case "24h" -> now.minus(24, ChronoUnit.HOURS);
            case "30d" -> now.minus(30, ChronoUnit.DAYS);
            default -> now.minus(7, ChronoUnit.DAYS);
        };
        return new TimeWindow(startTime, now);
    }

    private record TimeWindow(Instant startTime, Instant endTime) {}
}
