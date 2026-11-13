package com.gateflow.management_api.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.application.dto.ApplicationResponse;
import com.gateflow.management_api.application.dto.CreateApplicationRequest;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.application.exception.ApplicationAlreadyExistsException;
import com.gateflow.management_api.application.exception.ApplicationNotFoundException;
import com.gateflow.management_api.application.repository.ApplicationRepository;
import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OrganizationRepository organizationRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            OrganizationRepository organizationRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.organizationRepository = organizationRepository;
    }

    public ApplicationResponse createApplication(
            UUID organizationId,
            CreateApplicationRequest request
    ) {

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Organization not found: " + organizationId
                ));

        if (applicationRepository.existsByOrganizationIdAndSlug(
                organizationId, request.slug())) {

            throw new ApplicationAlreadyExistsException(
                    "Application slug already exists in this organization: "
                    + request.slug()
            );
        }

        Application application = new Application(
                organization,
                request.name(),
                request.slug(),
                request.description()
        );

        Application savedApplication = applicationRepository.save(application);

        return new ApplicationResponse(
                savedApplication.getId(),
                savedApplication.getOrganization().getId(),
                savedApplication.getName(),
                savedApplication.getSlug(),
                savedApplication.getDescription(),
                savedApplication.getStatus(),
                savedApplication.getCreatedAt(),
                savedApplication.getUpdatedAt()
        );

    }

    public ApplicationResponse getApplication(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        return new ApplicationResponse(
                application.getId(),
                application.getOrganization().getId(),
                application.getName(),
                application.getSlug(),
                application.getDescription(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    public List<ApplicationResponse> getApplicationsByOrganization(UUID organizationId) {

        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(
                    "Organization not found: " + organizationId
            );
        }

        return applicationRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(application -> new ApplicationResponse(
                        application.getId(),
                        application.getOrganization().getId(),
                        application.getName(),
                        application.getSlug(),
                        application.getDescription(),
                        application.getStatus(),
                        application.getCreatedAt(),
                        application.getUpdatedAt()
                ))
                .toList();
    }
}
