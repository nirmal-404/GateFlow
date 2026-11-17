package com.gateflow.gateway.apikey.model;

import java.util.UUID;

public record ApiKeyPolicyRecord(
        UUID id,
        UUID apiKeyId,
        int rateLimit,
        int rateLimitWindowSeconds,
        long quotaLimit,
        String quotaWindow,
        String policySource
) {
}