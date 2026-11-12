package com.gateflow.management_api.organization.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.organization.dto.CreateOrganizationRequest;
import com.gateflow.management_api.organization.dto.OrganizationResponse;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;


@Service
@Transactional 
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrganizationResponse createOrganization(
            CreateOrganizationRequest request
    ) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new OrganizationAlreadyExistsException(
                    "Organization slug already exists: " + request.slug()
            );
        }

        Organization organization = new Organization(
                request.name(),
                request.slug(),
                request.description()
        );

        Organization savedOrganization =
                organizationRepository.save(organization);

        return mapToResponse(savedOrganization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID id) {
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