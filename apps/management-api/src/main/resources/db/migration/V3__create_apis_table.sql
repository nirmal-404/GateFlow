CREATE TABLE apis (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    description VARCHAR(500),

    base_url VARCHAR(500) NOT NULL,
    version VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_apis_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_apis_organization_slug
        UNIQUE (organization_id, slug)
);