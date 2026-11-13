package com.gateflow.management_api.subscription.entity;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.api.entity.Api;
import com.gateflow.management_api.application.entity.Application;
import com.gateflow.management_api.organization.entity.Organization;

import jakarta.persistence.*;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_subscriptions_application_api",
                    columnNames = {"application_id", "api_id"}
            )
        }
)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
    }

    public Subscription(
            Organization organization,
            Application application,
            Api api
    ) {
        this.organization = organization;
        this.application = application;
        this.api = api;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Application getApplication() {
        return application;
    }

    public Api getApi() {
        return api;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

}
