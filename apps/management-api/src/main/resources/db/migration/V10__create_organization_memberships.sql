CREATE TABLE organization_memberships (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id         UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    role            VARCHAR (20) NOT NULL,
    created_at      TIMESTAMPTZ  DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uk_membership_user_organization UNIQUE (user_id, organization_id),
    CONSTRAINT chk_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'DEVELOPER', 'VIEWER'))
);

CREATE INDEX idx_membership_user
    ON organization_memberships(user_id);

CREATE INDEX idx_membership_organization
    ON organization_memberships(organization_id);