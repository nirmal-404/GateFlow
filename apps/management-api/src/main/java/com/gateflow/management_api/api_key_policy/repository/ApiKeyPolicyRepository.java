package com.gateflow.management_api.api_key_policy.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.api_key_policy.entity.ApiKeyPolicy;

public interface ApiKeyPolicyRepository
        extends JpaRepository<ApiKeyPolicy, UUID> {

    Optional<ApiKeyPolicy> findByApiKeyId(UUID apiKeyId);

    boolean existsByApiKeyId(UUID apiKeyId);
}
