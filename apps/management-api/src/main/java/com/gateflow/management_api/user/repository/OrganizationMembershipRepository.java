package com.gateflow.management_api.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.entity.OrganizationRole;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, UUID> {

    List<OrganizationMembership> findAllByUserId(UUID userId);

    List<OrganizationMembership> findAllByOrganizationId(UUID organizationId);

    Optional<OrganizationMembership> findByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );

    boolean existsByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );

    boolean existsByUserIdAndOrganizationIdAndRole(
            UUID userId,
            UUID organizationId,
            OrganizationRole role
    );
}
