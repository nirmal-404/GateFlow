package com.gateflow.management_api.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gateflow.management_api.api.dto.ApiResponse;
import com.gateflow.management_api.api.dto.CreateApiRequest;
import com.gateflow.management_api.api.entity.Api;
import com.gateflow.management_api.api.exception.ApiAlreadyExistsException;
import com.gateflow.management_api.api.repository.ApiRepository;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;
import com.gateflow.management_api.organization.repository.OrganizationRepository;

@Service
@Transactional
public class ApiService {

    private final ApiRepository apiRepository;
    private final OrganizationRepository organizationRepository;

    public ApiService(
        ApiRepository apiRepository,
        OrganizationRepository organizationRepository
    ) {
        this.apiRepository = apiRepository;
        this.organizationRepository = organizationRepository;
    }

    public ApiResponse createApi(UUID organizationId, CreateApiRequest request) {

        var organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization not found: " + organizationId
            ));

        if (apiRepository.existsByOrganizationIdAndSlug(
            organizationId, request.slug()
        )) {
            throw new ApiAlreadyExistsException(
                "API slug already exists for this organization: " + request.slug()
            );
        }

        Api api = new Api(
            organization,
            request.name(),
            request.slug(),
            request.description(),
            request.baseUrl(),
            request.version()
        );

        Api savedApi = apiRepository.save(api);

        return mapToResponse(savedApi);
    }

    private ApiResponse mapToResponse(Api api) {
        return new ApiResponse(
            api.getId(),
            api.getOrganization().getId(),
            api.getName(),
            api.getSlug(),
            api.getDescription(),
            api.getBaseUrl(),
            api.getVersion(),
            api.getStatus(),
            api.getCreatedAt(),
            api.getUpdatedAt()
        );
    }
}
