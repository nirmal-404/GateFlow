package com.gateflow.management_api.organization.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gateflow.management_api.organization.dto.CreateOrganizationRequest;
import com.gateflow.management_api.organization.dto.OrganizationResponse;
import com.gateflow.management_api.organization.service.OrganizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        OrganizationResponse response =
                organizationService.createOrganization(request);

        URI location = URI.create(
                "/api/v1/organizations/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @PathVariable UUID id
    ) {
        OrganizationResponse response =
                organizationService.getOrganization(id);

        return ResponseEntity.ok(response);
    }
}
