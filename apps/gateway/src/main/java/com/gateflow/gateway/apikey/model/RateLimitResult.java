package com.gateflow.gateway.apikey.model;

import java.time.Duration;

public record RateLimitResult(
        boolean allowed,
        long currentRequests,
        long remainingRequests,
        Duration retryAfter
        ) {

}
