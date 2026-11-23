package com.gateflow.management_api.analytics.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.service.AnalyticsService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/analytics")
@Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewMetricsResponse> getOverview(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "7d") String range
    ) {
        OverviewMetricsResponse response = analyticsService.getOverviewMetrics(organizationId, range);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/traffic")
    public ResponseEntity<TrafficAnalyticsResponse> getTraffic(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(required = false) String interval
    ) {
        TrafficAnalyticsResponse response = analyticsService.getTrafficAnalytics(organizationId, range, interval);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-apis")
    public ResponseEntity<List<TopApiResponse>> getTopApis(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int limit
    ) {
        List<TopApiResponse> response = analyticsService.getTopApis(organizationId, range, limit);
        return ResponseEntity.ok(response);
    }
}
