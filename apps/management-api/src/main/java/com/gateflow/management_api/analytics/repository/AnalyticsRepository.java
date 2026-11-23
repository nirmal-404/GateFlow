package com.gateflow.management_api.analytics.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.gateflow.management_api.analytics.dto.OverviewMetricsResponse;
import com.gateflow.management_api.analytics.dto.TopApiResponse;
import com.gateflow.management_api.analytics.dto.TrafficAnalyticsResponse;
import com.gateflow.management_api.analytics.dto.TrafficBucketPoint;
import com.gateflow.management_api.analytics.dto.UsageSummary;

public interface AnalyticsRepository {

    UsageSummary getUsageSummary(UUID organizationId, Instant startTime, Instant endTime);

    OverviewMetricsResponse getOverviewMetrics(UUID organizationId, Instant startTime, Instant endTime, String period);

    List<TrafficBucketPoint> getTrafficBuckets(UUID organizationId, Instant startTime, Instant endTime, String bucketUnit);

    TrafficAnalyticsResponse getTrafficAnalytics(UUID organizationId, Instant startTime, Instant endTime, String bucketUnit);

    List<TopApiResponse> getTopApis(UUID organizationId, Instant startTime, Instant endTime, int limit);

    long countActiveApiKeys(UUID organizationId);
}
