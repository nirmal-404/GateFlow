CREATE TABLE subscriptions (
    id             UUID         PRIMARY KEY,
    application_id UUID         NOT NULL,
    api_id         UUID         NOT NULL,
    status         VARCHAR (20) DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at     TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_subscriptions_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_subscriptions_api FOREIGN KEY (api_id) REFERENCES apis (id),
    CONSTRAINT uk_subscriptions_application_api UNIQUE (application_id, api_id)
);