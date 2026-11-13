package com.gateflow.management_api.api_key.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.api_key.dto.ApiKeyResponse;
import com.gateflow.management_api.api_key.dto.CreateApiKeyRequest;
import com.gateflow.management_api.api_key.dto.CreateApiKeyResponse;
import com.gateflow.management_api.api_key.entity.ApiKey;
import com.gateflow.management_api.api_key.entity.ApiKeyStatus;
import com.gateflow.management_api.api_key.exception.ApiKeyAlreadyRevokedException;
import com.gateflow.management_api.api_key.exception.ApiKeyNotFoundException;
import com.gateflow.management_api.api_key.repository.ApiKeyRepository;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.application.exception.ApplicationNotFoundException;
import com.gateflow.management_api.application.repository.ApplicationRepository;

@Service
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public ApiKeyService(
            ApiKeyRepository apiKeyRepository,
            ApplicationRepository applicationRepository,
            ApiKeyGenerator apiKeyGenerator
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    public CreateApiKeyResponse createApiKey(
            UUID applicationId,
            CreateApiKeyRequest request
    ) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        ApiKeyGenerator.GeneratedApiKey generatedKey;

        do {
            generatedKey = apiKeyGenerator.generate(request.environment());
        } while (apiKeyRepository.existsByKeyPrefix(generatedKey.prefix()));

        ApiKey apiKey = new ApiKey(
                application,
                request.name(),
                request.environment(),
                generatedKey.prefix(),
                generatedKey.hash(),
                request.expiresAt()
        );

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        return new CreateApiKeyResponse(
                toResponse(savedApiKey),
                generatedKey.fullKey()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeysByApplication(UUID applicationId) {

        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(
                    "Application not found: " + applicationId
            );
        }

        return apiKeyRepository.findAllByApplicationId(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApiKeyResponse revokeApiKey(
            UUID applicationId,
            UUID apiKeyId
    ) {

        ApiKey apiKey = apiKeyRepository
                .findByIdAndApplicationId(apiKeyId, applicationId)
                .orElseThrow(() -> new ApiKeyNotFoundException(
                        "API key not found: " + apiKeyId
                ));

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            throw new ApiKeyAlreadyRevokedException(
                    "API key is already revoked: " + apiKeyId
            );
        }

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(Instant.now());

        return toResponse(apiKey);
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {

        String maskedKey = "gtf_"
                + apiKey.getEnvironment().name().toLowerCase()
                + "_"
                + apiKey.getKeyPrefix()
                + "_••••••••";

        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getApplication().getId(),
                apiKey.getName(),
                apiKey.getEnvironment(),
                apiKey.getKeyPrefix(),
                maskedKey,
                apiKey.getStatus(),
                apiKey.getExpiresAt(),
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt(),
                apiKey.getRevokedAt()
        );
    }
}
