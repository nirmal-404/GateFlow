package com.gateflow.gateway.apikey.model;

import java.time.Duration;

public record QuotaLimitResult(
        boolean allowed,
        long currentRequests,
        long remainingRequests,
        Duration retryAfter
        ) {}
