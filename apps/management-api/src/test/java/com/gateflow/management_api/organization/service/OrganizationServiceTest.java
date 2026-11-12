package com.gateflow.management_api.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateflow.management_api.organization.dto.CreateOrganizationRequest;
import com.gateflow.management_api.organization.dto.OrganizationResponse;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void shouldCreateOrganizationWhenSlugDoesNotExist() {

        // Arrange
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Acme Technologies",
                "acme",
                "Technology company"
        );

        when(organizationRepository.existsBySlug("acme"))
                .thenReturn(false);

        Organization savedOrganization = new Organization(
                "Acme Technologies",
                "acme",
                "Technology company"
        );

        when(organizationRepository.save(any(Organization.class)))
                .thenReturn(savedOrganization);

        // Act
        OrganizationResponse result
                = organizationService.createOrganization(request);

        // Assert
        assertEquals("acme", result.slug());
        assertEquals("Acme Technologies", result.name());
        assertEquals("Technology company", result.description());

        verify(organizationRepository).save(any(Organization.class));
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
    }
}
