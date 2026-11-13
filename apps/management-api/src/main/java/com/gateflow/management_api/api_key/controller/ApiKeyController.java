
package com.gateflow.management_api.api_key.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gateflow.management_api.api_key.dto.ApiKeyResponse;
import com.gateflow.management_api.api_key.dto.CreateApiKeyRequest;
import com.gateflow.management_api.api_key.dto.CreateApiKeyResponse;
import com.gateflow.management_api.api_key.service.ApiKeyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateApiKeyResponse createApiKey(
        @PathVariable UUID applicationId,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        return apiKeyService.createApiKey(applicationId, request);
    }

    @GetMapping
    public List<ApiKeyResponse> getApiKeys(
        @PathVariable UUID applicationId
    ) {
        return apiKeyService.getApiKeysByApplication(applicationId);
    }

    @PatchMapping("/{apiKeyId}/revoke")
    public ApiKeyResponse revokeApiKey(
        @PathVariable UUID applicationId,
        @PathVariable UUID apiKeyId
    ) {
        return apiKeyService.revokeApiKey(applicationId, apiKeyId);
    }
}
