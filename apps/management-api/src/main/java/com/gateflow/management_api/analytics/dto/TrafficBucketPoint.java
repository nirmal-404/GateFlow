package com.gateflow.management_api.analytics.dto;

import java.time.Instant;

public record TrafficBucketPoint(
        Instant bucketTime,
        long total,
        long successful,
        long failed
) {}
