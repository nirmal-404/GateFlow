package com.gateflow.gateway.apikey.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;
import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.RateLimitResult;
import com.gateflow.gateway.apikey.service.ApiKeyPolicyService;
import com.gateflow.gateway.apikey.exception.RedisRateLimitException;
import com.gateflow.gateway.apikey.service.RedisRateLimitService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ApiKeyRateLimitFilterTest {

    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";

    @Mock
    private ApiKeyPolicyService policyService;

    @Mock
    private RedisRateLimitService rateLimitService;

    private ApiKeyRateLimitFilter filter;

    private ApiKeyRecord apiKey;
    private ApiKeyPolicyRecord policy;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyRateLimitFilter(
                policyService,
                rateLimitService
        );

        apiKey = new ApiKeyRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "0123456789abcdef",
                "hashed-secret",
                "live",
                "ACTIVE",
                null
        );

        policy = new ApiKeyPolicyRecord(
                UUID.randomUUID(),
                apiKey.id(),
                100,
                60,
                10000,
                "DAILY",
                "DEFAULT"
        );
    }

    @Test
    void shouldRejectRequestWhenApiKeyIsMissing() {
        MockServerWebExchange exchange = createExchange();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, createChain(chainCalled)))
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        assertFalse(chainCalled.get());
        verifyNoInteractions(policyService, rateLimitService);
    }

    @Test
    void shouldReturnInternalServerErrorWhenPolicyIsMissing() {
        MockServerWebExchange exchange = createExchange();
        exchange.getAttributes().put(API_KEY_ATTRIBUTE, apiKey);

        AtomicBoolean chainCalled = new AtomicBoolean(false);

        when(policyService.findByApiKeyId(apiKey.id()))
                .thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, createChain(chainCalled)))
                .verifyComplete();

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exchange.getResponse().getStatusCode()
        );

        assertFalse(chainCalled.get());
        verify(policyService).findByApiKeyId(apiKey.id());
        verifyNoInteractions(rateLimitService);
    }

    @Test
    void shouldContinueRequestWhenWithinRateLimit() {
        MockServerWebExchange exchange = createAuthenticatedExchange();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        when(policyService.findByApiKeyId(apiKey.id()))
                .thenReturn(Mono.just(policy));

        when(rateLimitService.checkRateLimit(
                apiKey.id(),
                policy.rateLimit(),
                policy.rateLimitWindowSeconds()
        )).thenReturn(Mono.just(
                new RateLimitResult(
                        true,
                        10,
                        90,
                        Duration.ofSeconds(60)
                )
        ));

        StepVerifier.create(filter.filter(exchange, createChain(chainCalled)))
                .verifyComplete();

        assertTrue(chainCalled.get());
        assertEquals(
                "100",
                exchange.getResponse().getHeaders()
                        .getFirst("X-RateLimit-Limit")
        );
        assertEquals(
                "90",
                exchange.getResponse().getHeaders()
                        .getFirst("X-RateLimit-Remaining")
        );

        verify(rateLimitService).checkRateLimit(
                apiKey.id(),
                100,
                60
        );
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded() {
        MockServerWebExchange exchange = createAuthenticatedExchange();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        when(policyService.findByApiKeyId(apiKey.id()))
                .thenReturn(Mono.just(policy));

        when(rateLimitService.checkRateLimit(
                apiKey.id(),
                policy.rateLimit(),
                policy.rateLimitWindowSeconds()
        )).thenReturn(Mono.just(
                new RateLimitResult(
                        false,
                        101,
                        0,
                        Duration.ofSeconds(30)
                )
        ));

        StepVerifier.create(filter.filter(exchange, createChain(chainCalled)))
                .verifyComplete();

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exchange.getResponse().getStatusCode()
        );

        assertEquals(
                "30",
                exchange.getResponse().getHeaders()
                        .getFirst("Retry-After")
        );

        assertFalse(chainCalled.get());
    }

    @Test
    void shouldReturnServiceUnavailableWhenRedisFails() {
        MockServerWebExchange exchange = createAuthenticatedExchange();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        when(policyService.findByApiKeyId(apiKey.id()))
                .thenReturn(Mono.just(policy));

        when(rateLimitService.checkRateLimit(
                apiKey.id(),
                policy.rateLimit(),
                policy.rateLimitWindowSeconds()
        )).thenReturn(Mono.error(
                new RedisRateLimitException(
                        "Redis unavailable",
                        new RuntimeException("Connection refused")
                )
        ));

        StepVerifier.create(filter.filter(exchange, createChain(chainCalled)))
                .verifyComplete();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exchange.getResponse().getStatusCode()
        );

        assertFalse(chainCalled.get());
    }

    private MockServerWebExchange createExchange() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );

        Route route = mock(Route.class);
        when(route.getId()).thenReturn("public-api");

        exchange.getAttributes().put(
                ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                route
        );

        return exchange;
    }

    private MockServerWebExchange createAuthenticatedExchange() {
        MockServerWebExchange exchange = createExchange();
        exchange.getAttributes().put(API_KEY_ATTRIBUTE, apiKey);
        return exchange;
    }

    private GatewayFilterChain createChain(AtomicBoolean chainCalled) {
        return exchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };
    }
}
