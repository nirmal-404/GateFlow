package com.gateflow.management_api.api_key.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.gateflow.management_api.api_key.dto.CreateApiKeyRequest;
import com.gateflow.management_api.api_key.dto.CreateApiKeyResponse;
import com.gateflow.management_api.api_key.entity.ApiKey;
import com.gateflow.management_api.api_key.entity.ApiKeyEnvironment;
import com.gateflow.management_api.api_key.entity.ApiKeyStatus;
import com.gateflow.management_api.api_key.exception.ApiKeyAlreadyRevokedException;
import com.gateflow.management_api.api_key.exception.ApiKeyNotFoundException;
import com.gateflow.management_api.api_key.repository.ApiKeyRepository;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.application.entity.ApplicationStatus;
import com.gateflow.management_api.application.exception.ApplicationInactiveException;
import com.gateflow.management_api.application.repository.ApplicationRepository;
import com.gateflow.management_api.api_key_policy.config.DefaultApiKeyPolicyProperties;
import com.gateflow.management_api.api_key_policy.entity.ApiKeyPolicy;
import com.gateflow.management_api.api_key_policy.entity.PolicySource;
import com.gateflow.management_api.api_key_policy.entity.QuotaWindow;
import com.gateflow.management_api.api_key_policy.repository.ApiKeyPolicyRepository;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ApiKeyPolicyRepository apiKeyPolicyRepository;

    @Mock
    private DefaultApiKeyPolicyProperties defaultPolicyProperties;

    @Mock
    private OrganizationAuthorizationService authorizationService;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    void shouldRejectApiKeyCreationForInactiveApplication() {

        UUID applicationId = UUID.randomUUID();

        Application application = new Application(
                null,
                "Test Application",
                "test-application",
                "Test description"
        );

        ReflectionTestUtils.setField(
                application,
                "status",
                ApplicationStatus.INACTIVE
        );

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        assertThrows(
                ApplicationInactiveException.class,
                () -> apiKeyService.createApiKey(applicationId, null)
        );

        verifyNoInteractions(
                authorizationService,
                apiKeyGenerator,
                apiKeyRepository,
                apiKeyPolicyRepository
        );
    }

    @Test
    void shouldCreateApiKeyForActiveApplication() {

        UUID applicationId = UUID.randomUUID();

        UUID organizationId = UUID.randomUUID();

        Organization organization = mock(Organization.class);

        when(organization.getId()).thenReturn(organizationId);

        Application application = new Application(
                organization,
                "Test Application",
                "test-application",
                "Test description"
        );

        CreateApiKeyRequest request = new CreateApiKeyRequest(
                "Production Key",
                ApiKeyEnvironment.LIVE,
                null
        );

        ApiKeyGenerator.GeneratedApiKey generatedKey
                = new ApiKeyGenerator.GeneratedApiKey(
                        "prefix1234567890",
                        "secret-value",
                        "gtf_live_prefix1234567890_secret-value",
                        "hashed-secret"
                );

        ApiKey savedApiKey = new ApiKey(
                application,
                request.name(),
                request.environment(),
                generatedKey.prefix(),
                generatedKey.hash(),
                request.expiresAt()
        );

        UUID apiKeyId = UUID.randomUUID();
        ReflectionTestUtils.setField(savedApiKey, "id", apiKeyId);

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(apiKeyRepository.existsByKeyPrefix(generatedKey.prefix()))
                .thenReturn(false);

        when(apiKeyGenerator.generate(ApiKeyEnvironment.LIVE))
                .thenReturn(generatedKey);

        when(apiKeyRepository.save(any(ApiKey.class)))
                .thenReturn(savedApiKey);

        when(defaultPolicyProperties.getRateLimit()).thenReturn(100);
        when(defaultPolicyProperties.getRateLimitWindowSeconds()).thenReturn(60);
        when(defaultPolicyProperties.getQuotaLimit()).thenReturn(10000L);
        when(defaultPolicyProperties.getQuotaWindow()).thenReturn("DAILY");

        CreateApiKeyResponse result = apiKeyService.createApiKey(
                applicationId,
                request
        );

        assertEquals(generatedKey.fullKey(), result.secretKey());
        assertEquals(apiKeyId, result.apiKey().id());
        assertEquals(request.name(), result.apiKey().name());
        assertEquals(ApiKeyEnvironment.LIVE, result.apiKey().environment());

        verify(authorizationService).requirePermission(
                application.getOrganization().getId(),
                OrganizationPermission.CREATE_API_KEY
        );

        verify(apiKeyRepository).save(any(ApiKey.class));

        verify(apiKeyPolicyRepository).save(any(ApiKeyPolicy.class));
    }

    @Test
    void shouldRevokeActiveApiKey() {

        UUID applicationId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);

        Application application = mock(Application.class);
        when(application.getOrganization()).thenReturn(organization);
        when(application.getId()).thenReturn(applicationId);

        ApiKey apiKey = new ApiKey(
                application,
                "Production Key",
                ApiKeyEnvironment.LIVE,
                "prefix1234567890",
                "hashed-secret",
                null
        );

        apiKey.setId(apiKeyId);

        when(apiKeyRepository.findByIdAndApplicationId(apiKeyId, applicationId))
                .thenReturn(Optional.of(apiKey));

        apiKeyService.revokeApiKey(applicationId, apiKeyId);

        assertEquals(ApiKeyStatus.REVOKED, apiKey.getStatus());
        assertNotNull(apiKey.getRevokedAt());

        verify(authorizationService).requirePermission(
                organizationId,
                OrganizationPermission.REVOKE_API_KEY
        );
    }

    @Test
    void shouldRejectAlreadyRevokedApiKey() {

        UUID applicationId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();

        Application application = mock(Application.class);
        Organization organization = mock(Organization.class);

        when(application.getOrganization()).thenReturn(organization);

        ApiKey apiKey = new ApiKey(
                application,
                "Production Key",
                ApiKeyEnvironment.LIVE,
                "prefix1234567890",
                "hashed-secret",
                null
        );

        apiKey.setId(apiKeyId);
        apiKey.setStatus(ApiKeyStatus.REVOKED);

        when(apiKeyRepository.findByIdAndApplicationId(apiKeyId, applicationId))
                .thenReturn(Optional.of(apiKey));

        assertThrows(
                ApiKeyAlreadyRevokedException.class,
                () -> apiKeyService.revokeApiKey(applicationId, apiKeyId)
        );

        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void shouldRejectRevocationForAnotherApplication() {

        UUID applicationId = UUID.randomUUID();
        UUID apiKeyId = UUID.randomUUID();

        when(apiKeyRepository.findByIdAndApplicationId(apiKeyId, applicationId))
                .thenReturn(Optional.empty());

        assertThrows(
                ApiKeyNotFoundException.class,
                () -> apiKeyService.revokeApiKey(applicationId, apiKeyId)
        );

        verifyNoInteractions(authorizationService);
    }
}
