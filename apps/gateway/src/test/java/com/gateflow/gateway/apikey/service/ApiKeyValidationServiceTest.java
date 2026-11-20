package com.gateflow.gateway.apikey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.repository.ApiKeyRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationServiceTest {

    @Mock
    private ApiKeyParser parser;

    @Mock
    private ApiKeyHashService hashService;

    @Mock
    private ApiKeyRepository repository;

    @InjectMocks
    private ApiKeyValidationService validationService;

    @Test
    void shouldValidateValidApiKey() {

        String rawApiKey = "gtf_live_0123456789abcdef_testsecret";
        String secret = "testsecret";
        String keyHash = "hashed-secret";
        String prefix = "0123456789abcdef";

        ApiKeyParser.ParsedApiKey parsedKey
                = new ApiKeyParser.ParsedApiKey("live", prefix, secret);

        ApiKeyRecord apiKey = new ApiKeyRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                prefix,
                keyHash,
                "live",
                "ACTIVE",
                null
        );

        when(parser.parse(rawApiKey)).thenReturn(parsedKey);
        when(hashService.hashSecret(secret)).thenReturn(keyHash);
        when(repository.findValidApiKey(prefix, keyHash))
                .thenReturn(Mono.just(apiKey));

        StepVerifier.create(validationService.validate(rawApiKey))
                .assertNext(result -> assertEquals(apiKey, result))
                .verifyComplete();

        verify(parser).parse(rawApiKey);
        verify(hashService).hashSecret(secret);
        verify(repository).findValidApiKey(prefix, keyHash);
    }

    @Test
    void shouldRejectMalformedApiKey() {

        String rawApiKey = "invalid-api-key";

        when(parser.parse(rawApiKey)).thenReturn(null);

        StepVerifier.create(validationService.validate(rawApiKey))
                .verifyComplete();

        verifyNoInteractions(hashService, repository);
    }

    @Test
    void shouldRejectApiKeyWhenSecretDoesNotMatch() {

        String rawApiKey = "gtf_live_0123456789abcdef_wrongsecret";

        ApiKeyParser.ParsedApiKey parsedKey
                = new ApiKeyParser.ParsedApiKey(
                        "live",
                        "0123456789abcdef",
                        "wrongsecret"
                );

        when(parser.parse(rawApiKey)).thenReturn(parsedKey);
        when(hashService.hashSecret("wrongsecret"))
                .thenReturn("wrong-hash");

        when(repository.findValidApiKey(
                "0123456789abcdef",
                "wrong-hash"
        )).thenReturn(Mono.empty());

        StepVerifier.create(validationService.validate(rawApiKey))
                .verifyComplete();
    }

    @Test
    void shouldRejectApiKeyWhenEnvironmentDoesNotMatch() {

        String rawApiKey = "gtf_live_0123456789abcdef_testsecret";

        ApiKeyParser.ParsedApiKey parsedKey
                = new ApiKeyParser.ParsedApiKey(
                        "live",
                        "0123456789abcdef",
                        "testsecret"
                );

        ApiKeyRecord apiKey = new ApiKeyRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "0123456789abcdef",
                "hashed-secret",
                "test",
                "ACTIVE",
                null
        );

        when(parser.parse(rawApiKey)).thenReturn(parsedKey);
        when(hashService.hashSecret("testsecret"))
                .thenReturn("hashed-secret");

        when(repository.findValidApiKey(
                "0123456789abcdef",
                "hashed-secret"
        )).thenReturn(Mono.just(apiKey));

        StepVerifier.create(validationService.validate(rawApiKey))
                .verifyComplete();
    }
}
