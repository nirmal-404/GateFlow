package com.gateflow.gateway.apikey.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiRouteRecord;
import com.gateflow.gateway.apikey.service.ApiRouteResolutionService;

import reactor.core.publisher.Mono;

@Component
public class DynamicApiRoutingFilter implements GlobalFilter, Ordered {

    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";

    private final ApiRouteResolutionService routeResolutionService;

    public DynamicApiRoutingFilter(
            ApiRouteResolutionService routeResolutionService
    ) {
        this.routeResolutionService = routeResolutionService;
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

        String rawQuery = exchange.getRequest()
                .getURI()
                .getRawQuery();

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

        // Replace the default Gateway destination with our trusted DB destination.
        mutatedExchange.getAttributes().put(
                GATEWAY_REQUEST_URL_ATTR,
                targetUri
        );

        mutatedExchange.getAttributes().put(
                "gateflow.apiId",
                apiRoute.apiId()
        );

        return chain.filter(mutatedExchange);
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
                + ",\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);

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
