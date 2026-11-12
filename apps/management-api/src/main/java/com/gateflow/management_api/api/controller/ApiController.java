package com.gateflow.management_api.api.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gateflow.management_api.api.dto.ApiResponse;
import com.gateflow.management_api.api.dto.CreateApiRequest;
import com.gateflow.management_api.api.service.ApiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/apis")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createApi(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateApiRequest request
    ) {
        ApiResponse response = apiService.createApi(organizationId, request);

        URI location = URI.create(
                "/api/v1/organizations/" + organizationId + "/apis/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }
}
