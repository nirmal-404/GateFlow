package com.gateflow.management_api.api.entity;

import java.time.Instant;
import java.util.UUID;

import com.gateflow.management_api.organization.entity.Organization;

import jakarta.persistence.*;

@Entity
@Table(
    name = "apis",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_apis_organization_slug",
            columnNames = {"organization_id", "slug"}
        )
    }
)
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 20)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiStatus status = ApiStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Api() {
        // Required by JPA
    }

    public Api(
        Organization organization,
        String name,
        String slug,
        String description,
        String baseUrl,
        String version
    ) {
        this.organization = organization;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.baseUrl = baseUrl;
        this.version = version;
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

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getBaseUrl() { return baseUrl; }
    public String getVersion() { return version; }
    public ApiStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
