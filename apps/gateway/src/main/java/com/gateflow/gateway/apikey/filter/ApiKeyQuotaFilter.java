package com.gateflow.gateway.apikey.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiKeyPolicyRecord;
import com.gateflow.gateway.apikey.model.QuotaLimitResult;
import com.gateflow.gateway.apikey.service.ApiKeyQuotaService;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyQuotaFilter implements GlobalFilter, Ordered {

    private static final Logger log
            = LoggerFactory.getLogger(ApiKeyQuotaFilter.class);

    private static final String MANAGEMENT_ROUTE_ID = "management-api";
    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";
    private static final String API_KEY_POLICY_ATTRIBUTE = "gateflow.apiKeyPolicy";

    private final ApiKeyQuotaService quotaService;

    public ApiKeyQuotaFilter(ApiKeyQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @Override
    public int getOrder() {
        return 0;
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

        ApiKeyPolicyRecord policy
                = exchange.getAttribute(API_KEY_POLICY_ATTRIBUTE);

        if (apiKey == null || policy == null) {
            return writeError(
                    exchange,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "API key policy is not available"
            );
        }

        Mono<QuotaLimitResult> quotaCheck = quotaService.checkQuota(
                apiKey.id(),
                policy.quotaLimit(),
                policy.quotaWindow()
        ).onErrorMap(error -> new QuotaCheckException(error));

        return quotaCheck
                .flatMap(result -> {
                    addQuotaHeaders(exchange, result, policy);

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
                                "API quota exceeded"
                        );
                    }

                    return chain.filter(exchange);
                })
                .onErrorResume(QuotaCheckException.class, error -> {
                    log.error(
                            "Quota enforcement failed for API key {}",
                            apiKey.id(),
                            error.getCause()
                    );

                    return writeError(
                            exchange,
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "API quota service is temporarily unavailable"
                    );
                });
    }

    private void addQuotaHeaders(
            ServerWebExchange exchange,
            QuotaLimitResult result,
            ApiKeyPolicyRecord policy
    ) {
        var headers = exchange.getResponse().getHeaders();

        headers.set(
                "X-Quota-Limit",
                String.valueOf(policy.quotaLimit())
        );

        headers.set(
                "X-Quota-Remaining",
                String.valueOf(result.remainingRequests())
        );

        headers.set(
                "X-Quota-Reset",
                String.valueOf(
                        Instant.now()
                                .plus(result.retryAfter())
                                .getEpochSecond()
                )
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

    private static class QuotaCheckException extends RuntimeException {

        public QuotaCheckException(Throwable cause) {
            super(cause);
        }
    }
}
