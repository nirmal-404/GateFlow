package com.gateflow.gateway.apikey.filter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.service.ApiRouteResolutionService;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.handler.timeout.ReadTimeoutException;
import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Component
public class DynamicApiRoutingFilter implements GlobalFilter, Ordered {

    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";

    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);

    private final ApiRouteResolutionService routeResolutionService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public DynamicApiRoutingFilter(
            ApiRouteResolutionService routeResolutionService,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.routeResolutionService = routeResolutionService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        String[] segments = rawPath.split("/", 5);

        // Expected: /gateway/{slug}/{version}/{remaining-path}
        if (segments.length < 4
                || !"gateway".equals(segments[1])
                || !isValidSegment(segments[2])
                || !isValidSegment(segments[3])) {
            return writeError(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    "Invalid API gateway path"
            );
        }

        ApiKeyRecord apiKey = exchange.getAttribute(API_KEY_ATTRIBUTE);

        if (apiKey == null) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "API key authentication required"
            );
        }

        String slug = segments[2];
        String version = segments[3];

        String remainingPath = segments.length == 5
                ? "/" + segments[4]
                : "";

        return routeResolutionService.resolve(
                apiKey.applicationId(),
                slug,
                version
        ).flatMap(apiRoute
                -> forward(exchange, chain, apiRoute, remainingPath)
                        .thenReturn(true)
        ).defaultIfEmpty(false)
                .flatMap(found -> {
                    if (found) {
                        return Mono.empty();
                    }

                    return writeError(
                            exchange,
                            HttpStatus.NOT_FOUND,
                            "API not found or not accessible"
                    );
                });
    }

    private Mono<Void> forward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            ApiRouteRecord apiRoute,
            String remainingPath
    ) {
        URI upstreamBaseUri;

        try {
            upstreamBaseUri = URI.create(apiRoute.baseUrl());

            if (!upstreamBaseUri.isAbsolute()
                    || upstreamBaseUri.getHost() == null
                    || (!"http".equalsIgnoreCase(upstreamBaseUri.getScheme())
                    && !"https".equalsIgnoreCase(upstreamBaseUri.getScheme()))
                    || upstreamBaseUri.getUserInfo() != null
                    || upstreamBaseUri.getQuery() != null
                    || upstreamBaseUri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid upstream URL");
            }
        } catch (IllegalArgumentException exception) {
            return writeError(
                    exchange,
                    HttpStatus.BAD_GATEWAY,
                    "Registered API upstream URL is invalid"
            );
        }

        String baseUrl = apiRoute.baseUrl().replaceAll("/+$", "");
        String rawQuery = exchange.getRequest().getURI().getRawQuery();

        URI targetUri;

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .path(remainingPath);

            if (rawQuery != null) {
                uriBuilder.query(rawQuery);
            }

            targetUri = uriBuilder.build(true).toUri();

        } catch (IllegalArgumentException exception) {
            return writeError(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    "Invalid request URL"
            );
        }

        // Never forward the customer's API key to the upstream service.
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove("X-API-Key");
                    headers.remove("X-GateFlow-Application-Id");
                    headers.remove("X-GateFlow-Api-Id");
                }))
                .build();

        // Use the trusted upstream destination from the database.
        mutatedExchange.getAttributes().put(
                GATEWAY_REQUEST_URL_ATTR,
                targetUri
        );

        mutatedExchange.getAttributes().put(
                "gateflow.apiId",
                apiRoute.apiId()
        );

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
                "api-" + apiRoute.apiId()
        );

        Mono<Void> upstreamCall = Mono.defer(() -> {
            // Allow Gateway's routing filter to execute again on a retry.
            mutatedExchange.getAttributes().remove(
                    ServerWebExchangeUtils.GATEWAY_ALREADY_ROUTED_ATTR
            );

            return chain.filter(mutatedExchange);
        });

        // Retry only safe methods and known transport failures.
        if (isRetryableMethod(exchange.getRequest().getMethod())) {
            upstreamCall = upstreamCall.retryWhen(
                    Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                            .filter(this::isRetryableTransportError)
                            .onRetryExhaustedThrow(
                                    (spec, signal) -> signal.failure()
                            )
            );
        }

        // The circuit breaker counts the complete retry sequence as one call.
        return upstreamCall
                .transformDeferred(
                        CircuitBreakerOperator.of(circuitBreaker)
                )
                .onErrorResume(
                        CallNotPermittedException.class,
                        error -> writeError(
                                exchange,
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Upstream API is temporarily unavailable"
                        )
                );
    }

    private boolean isRetryableMethod(HttpMethod method) {
        return HttpMethod.GET.equals(method)
                || HttpMethod.HEAD.equals(method);
    }

    private boolean isRetryableTransportError(Throwable error) {
        Throwable current = error;

        while (current != null) {
            if (current instanceof IOException
                    || current instanceof TimeoutException
                    || current instanceof ReadTimeoutException
                    || current instanceof PrematureCloseException
                    || current instanceof AbortedException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean isValidSegment(String value) {
        return value != null
                && value.matches("[a-zA-Z0-9._-]+");
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String message
    ) {
        byte[] body = ("{\"status\":" + status.value()
                + ",\"error\":\"" + message + "\"}")
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
        return 10001;
    }
}
