ALTER TABLE api_key_policies
ADD COLUMN policy_source VARCHAR(20) NOT NULL DEFAULT 'DEFAULT'
CHECK (policy_source IN ('DEFAULT', 'CUSTOM'));