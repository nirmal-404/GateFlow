package com.gateflow.gateway.apikey.model;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyRecord(
        UUID id,
        UUID applicationId,
        String keyPrefix,
        String keyHash,
        String environment,
        String status,
        Instant expiresAt
        ) {

}
