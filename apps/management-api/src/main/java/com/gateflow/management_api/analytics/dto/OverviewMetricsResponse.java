package com.gateflow.management_api.analytics.dto;

public record OverviewMetricsResponse(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double successRate,
        double avgLatencyMs,
        Double totalRequestsChangePct,
        Double successRateChangePct,
        Double avgLatencyChangePct,
        long activeApiKeys,
        String period
) {}
