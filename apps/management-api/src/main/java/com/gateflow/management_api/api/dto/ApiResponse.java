package com.gateflow.management_api.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.api.entity.ApiStatus;

public record ApiResponse(
    UUID id,
    UUID organizationId,
    String name,
    String slug,
    String description,
    String baseUrl,
    String version,
    ApiStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
