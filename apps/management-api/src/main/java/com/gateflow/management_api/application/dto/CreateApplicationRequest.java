package com.gateflow.management_api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank(message = "Application name is required")
        @Size(max = 120, message = "Application name cannot exceed 120 characters")
        String name,
        @NotBlank(message = "Application slug is required")
        @Size(max = 80, message = "Application slug cannot exceed 80 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must contain lowercase letters, numbers, and hyphens only"
        )
        String slug,
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
        ) {

}
