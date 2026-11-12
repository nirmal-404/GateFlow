package com.gateflow.management_api.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

        @NotBlank(message = "Organization name is required")
        @Size(max = 120, message = "Name cannot exceed 120 characters")
        String name,

        @NotBlank(message = "Organization slug is required")
        @Size(max = 80, message = "Slug cannot exceed 80 characters")
        String slug,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description

) {
}
