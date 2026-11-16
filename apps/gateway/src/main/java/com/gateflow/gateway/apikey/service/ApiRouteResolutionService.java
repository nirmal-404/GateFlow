package com.gateflow.gateway.apikey.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.repository.ApiRouteRepository;

import reactor.core.publisher.Mono;

@Service
public class ApiRouteResolutionService {

    private final ApiRouteRepository repository;

    public ApiRouteResolutionService(ApiRouteRepository repository) {
        this.repository = repository;
    }

    public Mono<ApiRouteRecord> resolve(
            UUID applicationId,
            String slug,
            String version
    ) {
        return repository.findActiveSubscribedApi(
                applicationId,
                slug,
                version
        );
    }
}
