package com.gateflow.management_api.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.application.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);

    List<Application> findAllByOrganizationId(UUID organizationId);
}
