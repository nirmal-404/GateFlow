package com.gateflow.management_api.api_key_policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateflow.policy.defaults")
public class DefaultApiKeyPolicyProperties {

    private Integer rateLimit;
    private Integer rateLimitWindowSeconds;
    private Long quotaLimit;
    private String quotaWindow;

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Integer getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(Integer rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    public Long getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public String getQuotaWindow() {
        return quotaWindow;
    }

    public void setQuotaWindow(String quotaWindow) {
        this.quotaWindow = quotaWindow;
    }
}
