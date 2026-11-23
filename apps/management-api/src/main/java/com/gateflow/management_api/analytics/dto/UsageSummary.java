package com.gateflow.management_api.analytics.dto;

public record UsageSummary(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double avgLatencyMs
) {
    public static UsageSummary empty() {
        return new UsageSummary(0L, 0L, 0L, 0.0);
    }

    public double calculateSuccessRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return Math.round(((double) successfulRequests / totalRequests) * 10000.0) / 100.0;
    }
}
