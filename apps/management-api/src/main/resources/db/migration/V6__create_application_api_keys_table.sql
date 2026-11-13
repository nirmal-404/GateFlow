CREATE TABLE application_api_keys (
    id             UUID          PRIMARY KEY,
    application_id UUID          NOT NULL,
    name           VARCHAR (120) NOT NULL,
    environment    VARCHAR (10)  NOT NULL CHECK (environment IN ('LIVE', 'TEST')),
    key_prefix     VARCHAR (16)  NOT NULL UNIQUE,
    key_hash       VARCHAR (64)  NOT NULL UNIQUE,
    status         VARCHAR (20)  DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED')),
    expires_at     TIMESTAMPTZ  ,
    last_used_at   TIMESTAMPTZ  ,
    created_at     TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    revoked_at     TIMESTAMPTZ  ,
    CONSTRAINT fk_application_api_keys_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT ck_application_api_keys_revocation CHECK ((status = 'ACTIVE'
                                                          AND revoked_at IS NULL)
                                                         OR (status = 'REVOKED'
                                                             AND revoked_at IS NOT NULL))
);

CREATE INDEX idx_application_api_keys_application_id
    ON application_api_keys(application_id);