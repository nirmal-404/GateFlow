package com.gateflow.management_api.analytics.dto;

import java.util.List;

public record TrafficAnalyticsResponse(
        List<String> categories,
        List<Long> total,
        List<Long> successful,
        List<Long> failed
) {}
