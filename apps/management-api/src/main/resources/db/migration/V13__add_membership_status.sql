ALTER TABLE organization_memberships
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE organization_memberships
ADD CONSTRAINT chk_membership_status
CHECK (status IN ('ACTIVE', 'SUSPENDED'));