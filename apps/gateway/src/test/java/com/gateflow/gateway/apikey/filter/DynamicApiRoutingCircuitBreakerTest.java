package com.gateflow.gateway.apikey.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.service.ApiRouteResolutionService;
import com.gateflow.gateway.config.GatewayResilienceConfiguration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DynamicApiRoutingCircuitBreakerTest {

    private static final UUID API_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    private ApiRouteResolutionService routeResolutionService;
    private GatewayFilterChain chain;
    private DynamicApiRoutingFilter filter;
    private CircuitBreakerRegistry registry;
    private AtomicInteger upstreamSubscriptions;

    @BeforeEach
    void setUp() {

        routeResolutionService = mock(ApiRouteResolutionService.class);
        chain = mock(GatewayFilterChain.class);
        upstreamSubscriptions = new AtomicInteger();

        registry = new GatewayResilienceConfiguration()
                .circuitBreakerRegistry(Duration.ofMillis(500));

        filter = new DynamicApiRoutingFilter(
                routeResolutionService,
                registry
        );

        ApiRouteRecord route = new ApiRouteRecord(
                API_ID,
                UUID.randomUUID(),
                "orders",
                "v1",
                "http://localhost:9090"
        );

        when(routeResolutionService.resolve(
                APPLICATION_ID,
                "orders",
                "v1"
        )).thenReturn(Mono.just(route));
    }

    @Test
    void shouldOpenCircuitAfterRepeatedUpstreamFailures() {

        when(chain.filter(any()))
                .thenAnswer(invocation -> Mono.defer(() -> {
                    upstreamSubscriptions.incrementAndGet();
                    return Mono.error(
                            new RuntimeException("Upstream unavailable")
                    );
                }));

        for (int request = 0; request < 5; request++) {
            StepVerifier.create(filter.filter(createExchange(), chain))
                    .expectErrorMessage("Upstream unavailable")
                    .verify();
        }

        MockServerWebExchange rejectedExchange = createExchange();

        StepVerifier.create(filter.filter(rejectedExchange, chain))
                .verifyComplete();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                rejectedExchange.getResponse().getStatusCode()
        );

        assertEquals(5, upstreamSubscriptions.get());
    }

    @Test
    void shouldRecoverAfterSuccessfulHalfOpenRequests() throws InterruptedException {

        when(chain.filter(any()))
                .thenAnswer(invocation -> Mono.defer(() -> {
                    int request = upstreamSubscriptions.incrementAndGet();

                    if (request <= 5) {
                        return Mono.error(
                                new RuntimeException("Upstream unavailable")
                        );
                    }

                    return Mono.empty();
                }));

        // Open the circuit with five failures.
        for (int request = 0; request < 5; request++) {
            StepVerifier.create(filter.filter(createExchange(), chain))
                    .expectErrorMessage("Upstream unavailable")
                    .verify();
        }

        CircuitBreaker circuitBreaker = registry.circuitBreaker(
                "api-" + API_ID
        );

        assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        // Wait for automatic transition to HALF_OPEN.
        Thread.sleep(800);

        assertEquals(
                CircuitBreaker.State.HALF_OPEN,
                circuitBreaker.getState()
        );

        // Three successful trial requests should close the circuit.
        for (int request = 0; request < 3; request++) {
            StepVerifier.create(filter.filter(createExchange(), chain))
                    .verifyComplete();
        }

        assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        assertEquals(8, upstreamSubscriptions.get());
    }

    private MockServerWebExchange createExchange() {

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/gateway/orders/v1/test")
                        .header("X-API-Key", "test-api-key")
                        .build()
        );

        ApiKeyRecord apiKey = new ApiKeyRecord(
                UUID.randomUUID(),
                APPLICATION_ID,
                "test-prefix",
                "test-hash",
                "LIVE",
                "ACTIVE",
                Instant.now().plusSeconds(3600)
        );

        exchange.getAttributes().put("gateflow.apiKey", apiKey);

        return exchange;
    }
}
