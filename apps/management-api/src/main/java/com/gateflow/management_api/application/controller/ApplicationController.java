package com.gateflow.management_api.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gateflow.management_api.application.dto.ApplicationResponse;
import com.gateflow.management_api.application.dto.CreateApplicationRequest;
import com.gateflow.management_api.application.service.ApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/organizations/{organizationId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateApplicationRequest request) {

        return applicationService.createApplication(organizationId, request);
    }

    @GetMapping("/organizations/{organizationId}/applications")
    public List<ApplicationResponse> getApplicationsByOrganization(
            @PathVariable UUID organizationId) {

        return applicationService.getApplicationsByOrganization(organizationId);
    }

    @GetMapping("/applications/{applicationId}")
    public ApplicationResponse getApplication(
            @PathVariable UUID applicationId) {

        return applicationService.getApplication(applicationId);
    }
}
