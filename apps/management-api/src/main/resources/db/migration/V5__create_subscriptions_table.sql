ALTER TABLE apis
    ADD CONSTRAINT uq_apis_id_organization UNIQUE (id, organization_id);

ALTER TABLE applications
    ADD CONSTRAINT uq_applications_id_organization UNIQUE (id, organization_id);

CREATE TABLE subscriptions (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL,
    application_id  UUID         NOT NULL,
    api_id          UUID         NOT NULL,
    status          VARCHAR (20) DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at      TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_subscriptions_application FOREIGN KEY (application_id, organization_id) REFERENCES applications (id, organization_id),
    CONSTRAINT fk_subscriptions_api FOREIGN KEY (api_id, organization_id) REFERENCES apis (id, organization_id),
    CONSTRAINT uk_subscriptions_application_api UNIQUE (application_id, api_id)
);