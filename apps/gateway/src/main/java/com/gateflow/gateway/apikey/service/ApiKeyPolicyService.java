package com.gateflow.gateway.apikey.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;
import com.gateflow.gateway.apikey.repository.ApiKeyPolicyRepository;

import reactor.core.publisher.Mono;

@Service
public class ApiKeyPolicyService {

    private final ApiKeyPolicyRepository repository;

    public ApiKeyPolicyService(ApiKeyPolicyRepository repository) {
        this.repository = repository;
    }

    public Mono<ApiKeyPolicyRecord> findByApiKeyId(UUID apiKeyId) {
        return repository.findByApiKeyId(apiKeyId);
    }
}
