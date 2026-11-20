package com.gateflow.management_api.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateflow.management_api.organization.dto.CreateOrganizationRequest;
import com.gateflow.management_api.organization.dto.OrganizationResponse;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.service.CurrentUserService;
import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.entity.OrganizationRole;
import com.gateflow.management_api.user.entity.User;
import com.gateflow.management_api.user.repository.OrganizationMembershipRepository;
import com.gateflow.management_api.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OrganizationAuthorizationService authorizationService;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void shouldCreateOrganizationAndAssignCreatorAsOwner() {

        // Arrange
        UUID creatorId = UUID.randomUUID();
        User creator = mock(User.class);

        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Acme Technologies",
                "acme",
                "Technology company"
        );

        Organization savedOrganization = new Organization(
                "Acme Technologies",
                "acme",
                "Technology company"
        );

        when(organizationRepository.existsBySlug("acme"))
                .thenReturn(false);

        when(currentUserService.getCurrentUserId())
                .thenReturn(creatorId);

        when(userRepository.findById(creatorId))
                .thenReturn(Optional.of(creator));

        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrganization);

        // Act
        OrganizationResponse result
                = organizationService.createOrganization(request);

        // Assert organization
        assertEquals("acme", result.slug());
        assertEquals("Acme Technologies", result.name());
        assertEquals("Technology company", result.description());

        verify(organizationRepository).save(any(Organization.class));

        // Assert creator receives OWNER membership
        ArgumentCaptor<OrganizationMembership> membershipCaptor
                = ArgumentCaptor.forClass(OrganizationMembership.class);

        verify(membershipRepository).save(membershipCaptor.capture());

        OrganizationMembership membership = membershipCaptor.getValue();

        assertSame(creator, membership.getUser());
        assertSame(savedOrganization, membership.getOrganization());
        assertEquals(OrganizationRole.OWNER, membership.getRole());
    }

    @Test
    void shouldRejectOrganizationWhenSlugAlreadyExists() {

        // Arrange
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Acme Technologies",
                "acme",
                "Technology company"
        );

        when(organizationRepository.existsBySlug("acme"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(
                OrganizationAlreadyExistsException.class,
                () -> organizationService.createOrganization(request)
        );

        verify(organizationRepository, never())
                .save(any(Organization.class));

        verify(membershipRepository, never())
                .save(any(OrganizationMembership.class));
    }
}
