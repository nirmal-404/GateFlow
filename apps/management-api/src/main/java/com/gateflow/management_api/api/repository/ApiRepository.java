package com.gateflow.management_api.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.api.entity.Api;

public interface ApiRepository extends JpaRepository<Api, UUID> {

    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);

    Optional<Api> findByOrganizationIdAndSlug(UUID organizationId, String slug);
}
