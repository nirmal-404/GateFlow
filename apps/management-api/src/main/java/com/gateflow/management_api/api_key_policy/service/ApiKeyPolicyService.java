
package com.gateflow.management_api.api_key_policy.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.api_key.entity.ApiKey;
import com.gateflow.management_api.api_key.exception.ApiKeyNotFoundException;
import com.gateflow.management_api.api_key.repository.ApiKeyRepository;
import com.gateflow.management_api.api_key_policy.dto.ApiKeyPolicyResponse;
import com.gateflow.management_api.api_key_policy.dto.CreateApiKeyPolicyRequest;
import com.gateflow.management_api.api_key_policy.entity.ApiKeyPolicy;
import com.gateflow.management_api.api_key_policy.entity.PolicySource;
import com.gateflow.management_api.api_key_policy.exception.ApiKeyPolicyAlreadyExistsException;
import com.gateflow.management_api.api_key_policy.exception.ApiKeyPolicyNotFoundException;
import com.gateflow.management_api.api_key_policy.repository.ApiKeyPolicyRepository;

@Service
@Transactional
public class ApiKeyPolicyService {

    private final ApiKeyPolicyRepository policyRepository;
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyPolicyService(
            ApiKeyPolicyRepository policyRepository,
            ApiKeyRepository apiKeyRepository
    ) {
        this.policyRepository = policyRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKeyPolicyResponse createPolicy(
            UUID apiKeyId,
            CreateApiKeyPolicyRequest request
    ) {

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(
                        "API key not found: " + apiKeyId
                ));

        if (policyRepository.existsByApiKeyId(apiKeyId)) {
            throw new ApiKeyPolicyAlreadyExistsException(
                    "A policy already exists for API key: " + apiKeyId
            );
        }

        ApiKeyPolicy policy = new ApiKeyPolicy(
                apiKey,
                request.rateLimit(),
                request.rateLimitWindowSeconds(),
                request.quotaLimit(),
                request.quotaWindow(),
                PolicySource.CUSTOM
        );

        return toResponse(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public ApiKeyPolicyResponse getPolicy(UUID apiKeyId) {

        ApiKeyPolicy policy = policyRepository.findByApiKeyId(apiKeyId)
                .orElseThrow(() -> new ApiKeyPolicyNotFoundException(
                        "Policy not found for API key: " + apiKeyId
                ));

        return toResponse(policy);
    }

    public ApiKeyPolicyResponse updatePolicy(
            UUID apiKeyId,
            CreateApiKeyPolicyRequest request
    ) {

        ApiKeyPolicy policy = policyRepository.findByApiKeyId(apiKeyId)
                .orElseThrow(() -> new ApiKeyPolicyNotFoundException(
                        "Policy not found for API key: " + apiKeyId
                ));

        policy.setRateLimit(request.rateLimit());
        policy.setRateLimitWindowSeconds(request.rateLimitWindowSeconds());
        policy.setQuotaLimit(request.quotaLimit());
        policy.setQuotaWindow(request.quotaWindow());

        // An administrator has customized the default policy.
        policy.setPolicySource(PolicySource.CUSTOM);

        return toResponse(policy);
    }

    private ApiKeyPolicyResponse toResponse(ApiKeyPolicy policy) {

        return new ApiKeyPolicyResponse(
                policy.getId(),
                policy.getApiKey().getId(),
                policy.getRateLimit(),
                policy.getRateLimitWindowSeconds(),
                policy.getQuotaLimit(),
                policy.getQuotaWindow(),
                policy.getPolicySource(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
