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
import com.gateflow.management_api.security.authorization.OrganizationAuthorizationService;
import com.gateflow.management_api.security.authorization.OrganizationPermission;

@Service
@Transactional
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationAuthorizationService authorizationService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            OrganizationRepository organizationRepository,
            OrganizationAuthorizationService authorizationService
    ) {
        this.applicationRepository = applicationRepository;
        this.organizationRepository = organizationRepository;
        this.authorizationService = authorizationService;
    }

    public ApplicationResponse createApplication(
            UUID organizationId,
            CreateApplicationRequest request
    ) {
        authorizationService.requirePermission(
                organizationId,
                OrganizationPermission.CREATE_APPLICATION
        );

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

        return mapToResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId
                ));

        authorizationService.requirePermission(
                application.getOrganization().getId(),
                OrganizationPermission.VIEW_APPLICATION
        );

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByOrganization(
            UUID organizationId
    ) {
        authorizationService.requirePermission(
                organizationId,
                OrganizationPermission.VIEW_APPLICATION
        );

        return applicationRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ApplicationResponse mapToResponse(Application application) {
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
}
