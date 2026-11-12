package com.gateflow.management_api.organization.repository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.organization.entity.Organization;

@SpringBootTest
@Transactional
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void shouldSaveAndFindOrganizationBySlug() {

        // Arrange
        String slug = "acme-" + UUID.randomUUID();

        Organization organization = new Organization(
                "Acme Technologies",
                slug,
                "Technology company"
        );

        // Act
        Organization savedOrganization =
                organizationRepository.save(organization);

        var foundOrganization =
                organizationRepository.findBySlug(slug);

        // Assert
        assertTrue(foundOrganization.isPresent());

        assertEquals(
                savedOrganization.getId(),
                foundOrganization.get().getId()
        );

        assertEquals("Acme Technologies", foundOrganization.get().getName());
    }
}
