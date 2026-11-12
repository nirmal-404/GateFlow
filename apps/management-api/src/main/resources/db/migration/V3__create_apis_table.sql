CREATE TABLE apis (
    id              UUID          PRIMARY KEY,
    organization_id UUID          NOT NULL,
    name            VARCHAR (120) NOT NULL,
    slug            VARCHAR (80)  NOT NULL,
    description     VARCHAR (500),
    base_url        VARCHAR (500) NOT NULL,
    version         VARCHAR (20)  NOT NULL,
    status          VARCHAR (20)  DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at      TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_apis_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_apis_organization_slug UNIQUE (organization_id, slug)
);