package com.gateflow.management_api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiRequest(
        @NotBlank(message = "API name is required")
        @Size(max = 120)
        String name,
        @NotBlank(message = "API slug is required")
        @Size(max = 80)
        String slug,
        @Size(max = 500)
        String description,
        @NotBlank(message = "Base URL is required")
        @Size(max = 500)
        String baseUrl,
        @NotBlank(message = "API version is required")
        @Size(max = 20)
        String version
        ) {}
