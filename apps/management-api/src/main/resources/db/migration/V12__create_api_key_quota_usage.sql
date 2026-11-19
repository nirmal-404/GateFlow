CREATE TABLE api_key_quota_usage (
    api_key_id    UUID         NOT NULL,
    quota_window  VARCHAR (20) NOT NULL,
    window_start  TIMESTAMPTZ  NOT NULL,
    request_count BIGINT       DEFAULT 0 NOT NULL,
    updated_at    TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_api_key_quota_usage PRIMARY KEY (api_key_id, quota_window, window_start),
    CONSTRAINT fk_api_key_quota_usage_api_key FOREIGN KEY (api_key_id) REFERENCES application_api_keys (id),
    CONSTRAINT ck_api_key_quota_usage_window CHECK (quota_window IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_api_key_quota_usage_count CHECK (request_count >= 0)
);

CREATE INDEX idx_api_key_quota_usage_updated_at
    ON api_key_quota_usage(updated_at);