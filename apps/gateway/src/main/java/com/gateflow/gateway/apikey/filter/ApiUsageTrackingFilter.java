package com.gateflow.gateway.apikey.filter;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.gateflow.gateway.apikey.model.ApiKeyRecord;
import com.gateflow.gateway.apikey.model.ApiUsageEvent;
import com.gateflow.gateway.apikey.service.ApiUsageEventPublisher;
import com.gateflow.gateway.apikey.repository.ApiUsageRepository;

import reactor.core.publisher.Mono;

@Component
public class ApiUsageTrackingFilter implements GlobalFilter, Ordered {

    private static final Logger log
            = LoggerFactory.getLogger(ApiUsageTrackingFilter.class);

    private static final String API_KEY_ATTRIBUTE = "gateflow.apiKey";
    private static final String API_ID_ATTRIBUTE = "gateflow.apiId";

    private final ApiUsageEventPublisher eventPublisher;
    private final ApiUsageRepository usageRepository;

    public ApiUsageTrackingFilter(
            ApiUsageEventPublisher eventPublisher,
            ApiUsageRepository usageRepository
    ) {
        this.eventPublisher = eventPublisher;
        this.usageRepository = usageRepository;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        long startTime = System.nanoTime();
        Instant requestedAt = Instant.now();

        return chain.filter(exchange)
                .then(Mono.defer(() -> {
                    ApiKeyRecord apiKey = exchange.getAttribute(
                            API_KEY_ATTRIBUTE
                    );

                    // Ignore requests that did not pass API-key authentication.
                    if (apiKey == null) {
                        return Mono.empty();
                    }

                    UUID apiId = exchange.getAttribute(API_ID_ATTRIBUTE);

                    var responseStatus = exchange.getResponse().getStatusCode();

                    int statusCode = responseStatus == null
                            ? 200
                            : responseStatus.value();

                    long durationMs = Math.max(
                            0,
                            (System.nanoTime() - startTime) / 1_000_000
                    );

                    ApiUsageEvent event = new ApiUsageEvent(
                            UUID.randomUUID(),
                            apiKey.id(),
                            apiId,
                            exchange.getRequest().getMethod().name(),
                            exchange.getRequest().getURI().getRawPath(),
                            statusCode,
                            durationMs,
                            requestedAt
                    );

                    return eventPublisher.publish(event)
                            .onErrorResume(redisError -> {
                                log.error(
                                        "Redis usage event publishing failed. "
                                        + "Attempting direct PostgreSQL persistence.",
                                        redisError
                                );

                                return usageRepository.save(event);
                            })
                            .onErrorResume(databaseError -> {
                                log.error(
                                        "Could not persist usage event {}",
                                        event.id(),
                                        databaseError
                                );

                                return Mono.empty();
                            });
                }));
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
