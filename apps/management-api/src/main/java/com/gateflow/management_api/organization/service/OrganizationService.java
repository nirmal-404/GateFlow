package com.gateflow.management_api.organization.service;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.organization.dto.CreateOrganizationRequest;
import com.gateflow.management_api.organization.dto.OrganizationResponse;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;
import com.gateflow.management_api.security.service.CurrentUserService;
import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.entity.OrganizationRole;
import com.gateflow.management_api.user.entity.User;
import com.gateflow.management_api.user.repository.OrganizationMembershipRepository;
import com.gateflow.management_api.user.repository.UserRepository;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationMembershipRepository membershipRepository,
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    ) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new OrganizationAlreadyExistsException(
                    "Organization slug already exists: " + request.slug()
            );
        }

        UUID creatorId = currentUserService.getCurrentUserId();

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user no longer exists"
                ));

        Organization organization = new Organization(
                request.name(),
                request.slug(),
                request.description()
        );

        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(creator);
        membership.setOrganization(savedOrganization);
        membership.setRole(OrganizationRole.OWNER);

        membershipRepository.save(membership);

        return mapToResponse(savedOrganization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID id) {

        authorizationService.requirePermission(
                id,
                OrganizationPermission.VIEW_ORGANIZATION
        );

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Organization not found: " + id
                ));

        return mapToResponse(organization);
    }

    private OrganizationResponse mapToResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getDescription(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }
}
