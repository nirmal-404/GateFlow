
package com.gateflow.gateway.apikey.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.service.ApiRouteResolutionService;
import com.gateflow.gateway.config.GatewayResilienceConfiguration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DynamicApiRoutingRetryTest {

    private static final UUID API_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private ApiRouteResolutionService routeResolutionService;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private DynamicApiRoutingFilter filter;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = new GatewayResilienceConfiguration()
                .circuitBreakerRegistry(Duration.ofSeconds(30));

        filter = new DynamicApiRoutingFilter(
                routeResolutionService,
                circuitBreakerRegistry
        );

        ApiRouteRecord route = new ApiRouteRecord(
                API_ID,
                UUID.randomUUID(),
                "orders",
                "v1",
                "http://localhost:9000"
        );

        when(routeResolutionService.resolve(
                APPLICATION_ID,
                "orders",
                "v1"
        )).thenReturn(Mono.just(route));
    }

    @Test
    void shouldRetryGetAndSucceedOnThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger();

        GatewayFilterChain chain = exchange -> Mono.defer(() -> {
            int attempt = attempts.incrementAndGet();

            if (attempt < 3) {
                return Mono.error(new IOException("Temporary upstream failure"));
            }

            return Mono.empty();
        });

        StepVerifier.create(filter.filter(createExchange(HttpMethod.GET), chain))
                .verifyComplete();

        assertEquals(3, attempts.get());
        assertEquals(
                0,
                circuitBreakerRegistry.circuitBreaker("api-" + API_ID)
                        .getMetrics().getNumberOfFailedCalls()
        );
    }

    @Test
    void shouldStopAfterExhaustingGetRetries() {
        AtomicInteger attempts = new AtomicInteger();

        GatewayFilterChain chain = exchange -> Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new IOException("Upstream unavailable"));
        });

        StepVerifier.create(filter.filter(createExchange(HttpMethod.GET), chain))
                .expectError(IOException.class)
                .verify();

        assertEquals(3, attempts.get());

        // The complete retry sequence counts as one failed circuit-breaker call.
        assertEquals(
                1,
                circuitBreakerRegistry.circuitBreaker("api-" + API_ID)
                        .getMetrics().getNumberOfFailedCalls()
        );
    }

    @Test
    void shouldNotRetryPostRequests() {
        AtomicInteger attempts = new AtomicInteger();

        GatewayFilterChain chain = exchange -> Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new IOException("Upstream unavailable"));
        });

        StepVerifier.create(filter.filter(createExchange(HttpMethod.POST), chain))
                .expectError(IOException.class)
                .verify();

        assertEquals(1, attempts.get());
        assertEquals(
                1,
                circuitBreakerRegistry.circuitBreaker("api-" + API_ID)
                        .getMetrics().getNumberOfFailedCalls()
        );
    }

    private MockServerWebExchange createExchange(HttpMethod method) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(
                        method,
                        "/gateway/orders/v1/products"
                ).build()
        );

        exchange.getAttributes().put(
                "gateflow.apiKey",
                new ApiKeyRecord(
                        UUID.randomUUID(),
                        APPLICATION_ID,
                        "test-prefix",
                        "test-hash",
                        "LIVE",
                        "ACTIVE",
                        null
                )
        );

        return exchange;
    }
}
