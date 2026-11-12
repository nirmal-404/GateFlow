package com.gateflow.management_api.organization.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.organization.entity.OrganizationStatus;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        String description,
        OrganizationStatus status,
        Instant createdAt,
        Instant updatedAt
        ) {

}
