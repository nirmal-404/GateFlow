package com.gateflow.management_api.api_key.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.api_key.entity.ApiKey;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    boolean existsByKeyPrefix(String keyPrefix);

    List<ApiKey> findAllByApplicationId(UUID applicationId);

    Optional<ApiKey> findByIdAndApplicationId(
            UUID id,
            UUID applicationId
    );
}
