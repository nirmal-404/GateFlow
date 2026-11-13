package com.gateflow.management_api.api_key.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.api_key.entity.ApiKeyEnvironment;
import com.gateflow.management_api.api_key.entity.ApiKeyStatus;

public record ApiKeyResponse(
        UUID id,
        UUID applicationId,
        String name,
        ApiKeyEnvironment environment,
        String keyPrefix,
        String maskedKey,
        ApiKeyStatus status,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant createdAt,
        Instant revokedAt
        ) {

}
