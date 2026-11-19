package com.gateflow.gateway.apikey.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gateflow.gateway.apikey.model.QuotaLimitResult;
import com.gateflow.gateway.apikey.repository.ApiKeyQuotaUsageRepository;

import reactor.core.publisher.Mono;

@Service
public class ApiKeyQuotaService {

    private final ApiKeyQuotaUsageRepository repository;

    public ApiKeyQuotaService(ApiKeyQuotaUsageRepository repository) {
        this.repository = repository;
    }

    public Mono<QuotaLimitResult> checkQuota(
            UUID apiKeyId,
            long quotaLimit,
            String quotaWindow
    ) {
        if (quotaLimit <= 0 || quotaWindow == null) {
            return Mono.error(
                    new IllegalArgumentException("Invalid API key quota configuration")
            );
        }

        Instant now = Instant.now();

        LocalDate today = now
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        LocalDate windowStartDate;
        LocalDate windowEndDate;

        switch (quotaWindow.toUpperCase(Locale.ROOT)) {
            case "DAILY" -> {
                windowStartDate = today;
                windowEndDate = today.plusDays(1);
            }

            case "WEEKLY" -> {
                windowStartDate = today.with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                );

                windowEndDate = windowStartDate.plusWeeks(1);
            }

            case "MONTHLY" -> {
                YearMonth currentMonth = YearMonth.from(today);

                windowStartDate = currentMonth.atDay(1);
                windowEndDate = currentMonth.plusMonths(1).atDay(1);
            }

            default -> {
                return Mono.error(
                        new IllegalArgumentException(
                                "Unsupported quota window: " + quotaWindow
                        )
                );
            }
        }

        Instant windowStart = windowStartDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant windowEnd = windowEndDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Duration retryAfter = Duration.between(now, windowEnd);

        return repository.reserve(
                apiKeyId,
                quotaWindow.toUpperCase(Locale.ROOT),
                windowStart,
                quotaLimit
        )
                .map(currentRequests -> new QuotaLimitResult(
                        true,
                        currentRequests,
                        Math.max(0, quotaLimit - currentRequests),
                        retryAfter
                ))
                .defaultIfEmpty(new QuotaLimitResult(
                        false,
                        quotaLimit,
                        0,
                        retryAfter
                ));
    }
}
