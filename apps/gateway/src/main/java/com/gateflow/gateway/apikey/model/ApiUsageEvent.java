package com.gateflow.gateway.apikey.model;

import java.time.Instant;
import java.util.UUID;

public record ApiUsageEvent(
        UUID id,
        UUID apiKeyId,
        UUID apiId,
        String httpMethod,
        String requestPath,
        int responseStatus,
        long durationMs,
        Instant requestedAt
        ) {}
