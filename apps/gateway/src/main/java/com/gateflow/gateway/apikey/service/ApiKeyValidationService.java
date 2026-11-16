package com.gateflow.gateway.apikey.service;

import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.repository.ApiKeyRepository;

import reactor.core.publisher.Mono;

@Service
public class ApiKeyValidationService {

    private final ApiKeyParser parser;
    private final ApiKeyHashService hashService;
    private final ApiKeyRepository repository;

    public ApiKeyValidationService(
            ApiKeyParser parser,
            ApiKeyHashService hashService,
            ApiKeyRepository repository
    ) {
        this.parser = parser;
        this.hashService = hashService;
        this.repository = repository;
    }

    public Mono<ApiKeyRecord> validate(String rawApiKey) {
        ApiKeyParser.ParsedApiKey parsedKey = parser.parse(rawApiKey);

        if (parsedKey == null) {
            return Mono.empty();
        }

        String keyHash = hashService.hashSecret(parsedKey.secret());

        return repository.findValidApiKey(
                parsedKey.prefix(),
                keyHash
        ).filter(apiKey
                -> apiKey.environment().equalsIgnoreCase(parsedKey.environment())
        );
    }
}
