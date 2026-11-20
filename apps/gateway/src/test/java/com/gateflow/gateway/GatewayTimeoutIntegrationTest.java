package com.gateflow.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;
import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.model.QuotaLimitResult;
import com.gateflow.gateway.apikey.model.RateLimitResult;
import com.gateflow.gateway.apikey.service.ApiKeyValidationService;
import com.gateflow.gateway.apikey.service.ApiKeyPolicyService;
import com.gateflow.gateway.apikey.service.RedisRateLimitService;
import com.gateflow.gateway.apikey.service.ApiKeyQuotaService;
import com.gateflow.gateway.apikey.service.ApiRouteResolutionService;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.gateway.server.webflux.httpclient.response-timeout=500ms"
        }
)
class GatewayTimeoutIntegrationTest {

    private static final UUID API_KEY_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    private static final String API_KEY = "test-api-key";

    private static final DisposableServer BACKEND = startSlowBackend();

    @LocalServerPort
    private int gatewayPort;

    @MockitoBean
    private ApiKeyValidationService validationService;

    @MockitoBean
    private ApiKeyPolicyService policyService;

    @MockitoBean
    private RedisRateLimitService rateLimitService;

    @MockitoBean
    private ApiKeyQuotaService quotaService;

    @MockitoBean
    private ApiRouteResolutionService routeResolutionService;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private static DisposableServer startSlowBackend() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes -> routes.get(
                        "/slow",
                        (request, response) -> response.sendString(
                                Mono.delay(Duration.ofSeconds(3))
                                        .map(ignored -> "Slow backend response")
                        )
                ))
                .bindNow();
    }

    @DynamicPropertySource
    static void configureGateway(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.cloud.gateway.server.webflux.routes[0].id",
                () -> "customer-api"
        );

        registry.add(
                "spring.cloud.gateway.server.webflux.routes[0].uri",
                () -> "http://127.0.0.1:" + BACKEND.port()
        );

        registry.add(
                "spring.cloud.gateway.server.webflux.routes[0].predicates[0]",
                () -> "Path=/gateway/**"
        );
    }

    @Test
    void shouldReturnGatewayTimeoutWhenBackendRespondsTooSlowly() {

        ApiKeyRecord apiKey = new ApiKeyRecord(
                API_KEY_ID,
                APPLICATION_ID,
                "testprefix",
                "testhash",
                "TEST",
                "ACTIVE",
                null
        );

        ApiKeyPolicyRecord policy = new ApiKeyPolicyRecord(
                UUID.randomUUID(),
                API_KEY_ID,
                100,
                60,
                1000L,
                "DAILY",
                "DEFAULT"
        );

        ApiRouteRecord apiRoute = new ApiRouteRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "orders",
                "v1",
                "http://127.0.0.1:" + BACKEND.port()
        );

        org.mockito.Mockito.when(validationService.validate(API_KEY))
                .thenReturn(Mono.just(apiKey));

        org.mockito.Mockito.when(policyService.findByApiKeyId(API_KEY_ID))
                .thenReturn(Mono.just(policy));

        org.mockito.Mockito.when(rateLimitService.checkRateLimit(
                API_KEY_ID, 100, 60
        )).thenReturn(Mono.just(
                new RateLimitResult(true, 1, 99, Duration.ofSeconds(60))
        ));

        org.mockito.Mockito.when(quotaService.checkQuota(
                API_KEY_ID, 1000, "DAILY"
        )).thenReturn(Mono.just(
                new QuotaLimitResult(true, 1, 999, Duration.ofDays(1))
        ));

        org.mockito.Mockito.when(routeResolutionService.resolve(
                APPLICATION_ID, "orders", "v1"
        )).thenReturn(Mono.just(apiRoute));

        try {
            WebClient.create()
                    .get()
                    .uri("http://localhost:" + gatewayPort
                            + "/gateway/orders/v1/slow")
                    .header("X-API-Key", API_KEY)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));

            org.junit.jupiter.api.Assertions.fail(
                    "Expected gateway timeout response"
            );

        } catch (WebClientResponseException exception) {
            assertEquals(504, exception.getStatusCode().value());
        }
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.dispose();
    }
}
