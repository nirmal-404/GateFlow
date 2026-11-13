CREATE TABLE api_key_policies (
    id                        UUID         PRIMARY KEY,
    api_key_id                UUID         NOT NULL UNIQUE,
    rate_limit                INT          NOT NULL,
    rate_limit_window_seconds INT          NOT NULL,
    quota_limit               BIGINT       NOT NULL,
    quota_window              VARCHAR (20) NOT NULL CHECK (quota_window IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    created_at                TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at                TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_api_key_policies_api_key FOREIGN KEY (api_key_id) REFERENCES application_api_keys (id),
    CONSTRAINT ck_api_key_policies_rate_limit CHECK (rate_limit > 0),
    CONSTRAINT ck_api_key_policies_rate_window CHECK (rate_limit_window_seconds > 0),
    CONSTRAINT ck_api_key_policies_quota CHECK (quota_limit > 0)
);