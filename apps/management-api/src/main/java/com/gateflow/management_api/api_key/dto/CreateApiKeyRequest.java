package com.gateflow.management_api.api_key.dto;

import java.time.Instant;

import com.gateflow.management_api.api_key.entity.ApiKeyEnvironment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        @NotNull
        ApiKeyEnvironment environment,
        @Future
        Instant expiresAt
        ) {

}
