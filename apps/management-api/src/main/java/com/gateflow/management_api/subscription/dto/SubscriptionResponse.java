package com.gateflow.management_api.subscription.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.subscription.entity.SubscriptionStatus;

public record SubscriptionResponse(
        UUID id,
        UUID organizationId,
        UUID applicationId,
        String applicationName,
        UUID apiId,
        String apiName,
        SubscriptionStatus status,
        Instant createdAt,
        Instant updatedAt
        ) {

}
