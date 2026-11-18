CREATE TABLE api_usage (
    id              UUID           PRIMARY KEY,
    api_key_id      UUID           NOT NULL,
    api_id          UUID          ,
    http_method     VARCHAR (10)   NOT NULL,
    request_path    VARCHAR (2048) NOT NULL,
    response_status INT            NOT NULL,
    duration_ms     BIGINT         NOT NULL,
    requested_at    TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_api_usage_api_key FOREIGN KEY (api_key_id) REFERENCES application_api_keys (id),
    CONSTRAINT fk_api_usage_api FOREIGN KEY (api_id) REFERENCES apis (id),
    CONSTRAINT ck_api_usage_duration CHECK (duration_ms >= 0),
    CONSTRAINT ck_api_usage_status CHECK (response_status BETWEEN 100 AND 599)
);

CREATE INDEX idx_api_usage_api_key_time
    ON api_usage(api_key_id, requested_at DESC);

CREATE INDEX idx_api_usage_api_time
    ON api_usage(api_id, requested_at DESC);

CREATE INDEX idx_api_usage_requested_at
    ON api_usage(requested_at DESC);