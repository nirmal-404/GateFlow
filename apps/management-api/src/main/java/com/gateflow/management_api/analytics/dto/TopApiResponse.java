package com.gateflow.management_api.analytics.dto;

import java.util.UUID;

public record TopApiResponse(
        UUID id,
        String name,
        String path,
        String method,
        long requests,
        double successRate,
        double avgLatencyMs
) {}
