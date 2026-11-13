package com.gateflow.management_api.api_key_policy.entity;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.api_key.entity.ApiKey;

import jakarta.persistence.*;

@Entity
@Table(name = "api_key_policies")
public class ApiKeyPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false, unique = true)
    private ApiKey apiKey;

    @Column(name = "rate_limit", nullable = false)
    private Integer rateLimit;

    @Column(name = "rate_limit_window_seconds", nullable = false)
    private Integer rateLimitWindowSeconds;

    @Column(name = "quota_limit", nullable = false)
    private Long quotaLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_window", nullable = false, length = 20)
    private QuotaWindow quotaWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_source", nullable = false, length = 20)
    private PolicySource policySource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApiKeyPolicy() {
    }

    public ApiKeyPolicy(
            ApiKey apiKey,
            Integer rateLimit,
            Integer rateLimitWindowSeconds,
            Long quotaLimit,
            QuotaWindow quotaWindow,
            PolicySource policySource) {

        this.apiKey = apiKey;
        this.rateLimit = rateLimit;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
        this.quotaLimit = quotaLimit;
        this.quotaWindow = quotaWindow;
        this.policySource = policySource;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

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

    public QuotaWindow getQuotaWindow() {
        return quotaWindow;
    }

    public void setQuotaWindow(QuotaWindow quotaWindow) {
        this.quotaWindow = quotaWindow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPolicySource(PolicySource policySource) {
        this.policySource = policySource;
    }
    
    public PolicySource getPolicySource() {
        return this.policySource;
    }
}
