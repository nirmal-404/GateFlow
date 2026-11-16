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

import com.gateflow.gateway.apikey.service.ApiKeyValidationService;

import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String MANAGEMENT_ROUTE_ID = "management-api";
    public static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";

    private final ApiKeyValidationService validationService;

    public ApiKeyAuthenticationFilter(ApiKeyValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        Route route = exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
        );

        // Management API uses JWT authentication instead.
        if (route == null || MANAGEMENT_ROUTE_ID.equals(route.getId())) {
            return chain.filter(exchange);
        }

        String rawApiKey = exchange.getRequest()
                .getHeaders()
                .getFirst(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            return unauthorized(exchange);
        }

        return validationService.validate(rawApiKey)
                .flatMap(apiKey -> {
                    exchange.getAttributes().put(API_KEY_ATTRIBUTE, apiKey);

                    return chain.filter(exchange).thenReturn(true);
                })
                .defaultIfEmpty(false)
                .flatMap(valid -> valid
                        ? Mono.empty()
                        : unauthorized(exchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        byte[] body = """
                {"error":"unauthorized","message":"Missing or invalid API key"}
                """.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
