package com.gateflow.management_api.api_key_policy.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.api_key_policy.entity.PolicySource;
import com.gateflow.management_api.api_key_policy.entity.QuotaWindow;

public record ApiKeyPolicyResponse(
        UUID id,
        UUID apiKeyId,
        Integer rateLimit,
        Integer rateLimitWindowSeconds,
        Long quotaLimit,
        QuotaWindow quotaWindow,
        PolicySource policySource,
        Instant createdAt,
        Instant updatedAt
) {}
