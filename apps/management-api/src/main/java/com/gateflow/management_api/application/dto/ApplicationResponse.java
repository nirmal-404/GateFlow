package com.gateflow.management_api.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.application.entity.ApplicationStatus;

public record ApplicationResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        String description,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
        ) {

}
