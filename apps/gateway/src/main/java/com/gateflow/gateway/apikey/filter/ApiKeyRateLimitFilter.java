package com.gateflow.gateway.apikey.filter;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.gateflow.gateway.apikey.exception.RedisRateLimitException;
import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;
import com.gateflow.gateway.apikey.model.RateLimitResult;
import com.gateflow.gateway.apikey.service.ApiKeyPolicyService;
import com.gateflow.gateway.apikey.service.RedisRateLimitService;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyRateLimitFilter implements GlobalFilter, Ordered {

    private static final String MANAGEMENT_ROUTE_ID = "management-api";
    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";
    private static final String API_KEY_POLICY_ATTRIBUTE = "gateflow.apiKeyPolicy";

    private final ApiKeyPolicyService policyService;
    private final RedisRateLimitService rateLimitService;

    public ApiKeyRateLimitFilter(
            ApiKeyPolicyService policyService,
            RedisRateLimitService rateLimitService
    ) {
        this.policyService = policyService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        Route route = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
        );

        if (route == null || MANAGEMENT_ROUTE_ID.equals(route.getId())) {
            return chain.filter(exchange);
        }

        ApiKeyRecord apiKey = exchange.getAttribute(API_KEY_ATTRIBUTE);

        if (apiKey == null) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "Missing API key authentication"
            );
        }

        return policyService.findByApiKeyId(apiKey.id())
                .flatMap(policy
                        -> checkLimit(exchange, chain, apiKey, policy)
                                .thenReturn(true)
                )
                .defaultIfEmpty(false)
                .flatMap(policyFound -> {
                    if (policyFound) {
                        return Mono.empty();
                    }

                    return writeError(
                            exchange,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "API key policy is not configured"
                    );
                });
    }

    private Mono<Void> checkLimit(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ApiKeyRecord apiKey,
            ApiKeyPolicyRecord policy
    ) {
        exchange.getAttributes().put(
                API_KEY_POLICY_ATTRIBUTE,
                policy
        );

        return rateLimitService.checkRateLimit(
                apiKey.id(),
                policy.rateLimit(),
                policy.rateLimitWindowSeconds()
        )
                .flatMap(result -> {
                    addRateLimitHeaders(exchange, result, policy);

                    if (!result.allowed()) {
                        exchange.getResponse().getHeaders().set(
                                "Retry-After",
                                String.valueOf(Math.max(
                                        1,
                                        result.retryAfter().toSeconds()
                                ))
                        );

                        return writeError(
                                exchange,
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Rate limit exceeded"
                        );
                    }

                    return chain.filter(exchange);
                })
                .onErrorResume(
                        RedisRateLimitException.class,
                        error -> writeError(
                                exchange,
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Rate limiting service is temporarily unavailable"
                        )
                );
    }

    private void addRateLimitHeaders(
            ServerWebExchange exchange,
            RateLimitResult result,
            ApiKeyPolicyRecord policy
    ) {
        var headers = exchange.getResponse().getHeaders();

        headers.set(
                "X-RateLimit-Limit",
                String.valueOf(policy.rateLimit())
        );

        headers.set(
                "X-RateLimit-Remaining",
                String.valueOf(result.remainingRequests())
        );

        headers.set(
                "X-RateLimit-Reset",
                String.valueOf(result.retryAfter().toSeconds())
        );
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String message
    ) {
        byte[] body = """
                {"error":"%s","message":"%s"}
                """.formatted(status.getReasonPhrase(), message)
                .getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(status);

        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
