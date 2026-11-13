package com.gateflow.management_api.api_key_policy.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gateflow.management_api.api_key_policy.dto.ApiKeyPolicyResponse;
import com.gateflow.management_api.api_key_policy.dto.CreateApiKeyPolicyRequest;
import com.gateflow.management_api.api_key_policy.service.ApiKeyPolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/api-keys/{apiKeyId}/policy")
public class ApiKeyPolicyController {

    private final ApiKeyPolicyService policyService;

    public ApiKeyPolicyController(ApiKeyPolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyPolicyResponse createPolicy(
            @PathVariable UUID apiKeyId,
            @Valid @RequestBody CreateApiKeyPolicyRequest request
    ) {
        return policyService.createPolicy(apiKeyId, request);
    }

    @GetMapping
    public ApiKeyPolicyResponse getPolicy(
            @PathVariable UUID apiKeyId
    ) {
        return policyService.getPolicy(apiKeyId);
    }

    @PutMapping
    public ApiKeyPolicyResponse updatePolicy(
            @PathVariable UUID apiKeyId,
            @Valid @RequestBody CreateApiKeyPolicyRequest request
    ) {
        return policyService.updatePolicy(apiKeyId, request);
    }
}
