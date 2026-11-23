package com.gateflow.management_api.analytics.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.repository.AnalyticsRepository;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private OrganizationAuthorizationService authorizationService;

    private final Instant fixedNow = Instant.parse("2026-11-27T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private AnalyticsService analyticsService;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository, authorizationService, fixedClock);
        organizationId = UUID.randomUUID();
    }

    @Test
    void getOverviewMetrics_withDefaultRange_shouldUse7d() {
        OverviewMetricsResponse mockResponse = new OverviewMetricsResponse(
                100L, 95L, 5L, 95.0, 120.0, 10.0, 2.0, -5.0, 3L, "7d"
        );
        when(analyticsRepository.getOverviewMetrics(eq(organizationId), any(), eq(fixedNow), eq("7d")))
                .thenReturn(mockResponse);

        OverviewMetricsResponse result = analyticsService.getOverviewMetrics(organizationId, null);

        assertNotNull(result);
        assertEquals(100L, result.totalRequests());
        assertEquals("7d", result.period());

        verify(authorizationService).requirePermission(organizationId, OrganizationPermission.VIEW_ORGANIZATION);
        verify(analyticsRepository).getOverviewMetrics(eq(organizationId), any(), eq(fixedNow), eq("7d"));
    }

    @Test
    void getOverviewMetrics_withValidRanges_shouldSucceed() {
        OverviewMetricsResponse mockResponse = new OverviewMetricsResponse(0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0, "24h");
        when(analyticsRepository.getOverviewMetrics(eq(organizationId), any(), eq(fixedNow), anyString()))
                .thenReturn(mockResponse);

        analyticsService.getOverviewMetrics(organizationId, "24h");
        analyticsService.getOverviewMetrics(organizationId, "7d");
        analyticsService.getOverviewMetrics(organizationId, "30d");
        analyticsService.getOverviewMetrics(organizationId, "7D"); // uppercase check

        verify(authorizationService, org.mockito.Mockito.times(4))
                .requirePermission(organizationId, OrganizationPermission.VIEW_ORGANIZATION);
    }

    @Test
    void getOverviewMetrics_withInvalidRange_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getOverviewMetrics(organizationId, "90d")
        );
        assertEquals("Invalid range: '90d'. Supported ranges are: 24h, 7d, 30d", exception.getMessage());
    }

    @Test
    void getTrafficAnalytics_withDefaultInterval_shouldInferSensibleInterval() {
        TrafficAnalyticsResponse mockResponse = new TrafficAnalyticsResponse(List.of(), List.of(), List.of(), List.of());
        when(analyticsRepository.getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), anyString()))
                .thenReturn(mockResponse);

        // 24h -> hour
        analyticsService.getTrafficAnalytics(organizationId, "24h", null);
        verify(analyticsRepository).getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), eq("hour"));

        // 7d -> day
        analyticsService.getTrafficAnalytics(organizationId, "7d", null);
        verify(analyticsRepository).getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), eq("day"));

        // 30d -> day
        analyticsService.getTrafficAnalytics(organizationId, "30d", "");
        verify(analyticsRepository, org.mockito.Mockito.times(2))
                .getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), eq("day"));
    }

    @Test
    void getTrafficAnalytics_withExplicitInterval_shouldUseRequestedInterval() {
        TrafficAnalyticsResponse mockResponse = new TrafficAnalyticsResponse(List.of(), List.of(), List.of(), List.of());
        when(analyticsRepository.getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), eq("week")))
                .thenReturn(mockResponse);

        analyticsService.getTrafficAnalytics(organizationId, "30d", "week");
        verify(analyticsRepository).getTrafficAnalytics(eq(organizationId), any(), eq(fixedNow), eq("week"));
    }

    @Test
    void getTrafficAnalytics_withInvalidInterval_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getTrafficAnalytics(organizationId, "7d", "minute")
        );
        assertEquals("Invalid interval: 'minute'. Supported intervals are: hour, day, week", exception.getMessage());
    }

    @Test
    void getTopApis_withDefaultLimit_shouldUse5() {
        List<TopApiResponse> mockResponse = List.of(
                new TopApiResponse(UUID.randomUUID(), "Test API", "/test", "GET", 50, 100.0, 45.0)
        );
        when(analyticsRepository.getTopApis(eq(organizationId), any(), eq(fixedNow), eq(5)))
                .thenReturn(mockResponse);

        List<TopApiResponse> result = analyticsService.getTopApis(organizationId, "7d", 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(analyticsRepository).getTopApis(eq(organizationId), any(), eq(fixedNow), eq(5));
    }

    @Test
    void getTopApis_withInvalidLimit_shouldThrowIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getTopApis(organizationId, "7d", 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getTopApis(organizationId, "7d", 101)
        );
    }
}
