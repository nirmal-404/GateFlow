CREATE TABLE applications (
    id              UUID          PRIMARY KEY,
    organization_id UUID          NOT NULL,
    name            VARCHAR (120) NOT NULL,
    slug            VARCHAR (80)  NOT NULL,
    description     VARCHAR (500),
    status          VARCHAR (20)  DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_applications_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_applications_organization_slug UNIQUE (organization_id, slug)
);