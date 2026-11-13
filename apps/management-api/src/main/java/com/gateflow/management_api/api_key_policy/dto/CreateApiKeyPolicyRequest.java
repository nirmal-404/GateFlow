package com.gateflow.management_api.api_key_policy.dto;

import com.gateflow.management_api.api_key_policy.entity.QuotaWindow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateApiKeyPolicyRequest(
        @NotNull
        @Positive
        Integer rateLimit,
        @NotNull
        @Positive
        Integer rateLimitWindowSeconds,
        @NotNull
        @Positive
        Long quotaLimit,
        @NotNull
        QuotaWindow quotaWindow
        ) {

}
