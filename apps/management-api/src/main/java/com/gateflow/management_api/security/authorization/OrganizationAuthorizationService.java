package com.gateflow.management_api.security.authorization;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.organization.entity.OrganizationStatus;
import com.gateflow.management_api.security.service.CurrentUserService;
import com.gateflow.management_api.user.entity.MembershipStatus;
import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.repository.OrganizationMembershipRepository;

@Service
@Transactional(readOnly = true)
public class OrganizationAuthorizationService {

    private final CurrentUserService currentUserService;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationAuthorizationService(
            CurrentUserService currentUserService,
            OrganizationMembershipRepository membershipRepository
    ) {
        this.currentUserService = currentUserService;
        this.membershipRepository = membershipRepository;
    }

    public OrganizationMembership requireMembership(UUID organizationId) {

        UUID userId = currentUserService.getCurrentUserId();

        OrganizationMembership membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new AccessDeniedException(
                        "You are not a member of this organization"
                ));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Your organization membership is suspended"
            );
        }

        if (membership.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "This organization is currently suspended"
            );
        }

        return membership;
    }

    public OrganizationMembership requirePermission(
            UUID organizationId,
            OrganizationPermission permission
    ) {

        OrganizationMembership membership = requireMembership(organizationId);

        if (!permission.isAllowedFor(membership.getRole())) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this operation"
            );
        }

        return membership;
    }

    public boolean hasPermission(
            UUID organizationId,
            OrganizationPermission permission
    ) {

        try {
            return permission.isAllowedFor(
                    requireMembership(organizationId).getRole()
            );
        } catch (AccessDeniedException exception) {
            return false;
        }
    }
}
