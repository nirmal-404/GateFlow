package com.gateflow.management_api.security.authorization;

import java.util.Set;

import com.gateflow.management_api.user.entity.OrganizationRole;

public enum OrganizationPermission {

    VIEW_ORGANIZATION(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER,
            OrganizationRole.VIEWER
    ),
    CREATE_APPLICATION(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER
    ),
    VIEW_APPLICATION(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER,
            OrganizationRole.VIEWER
    ),
    CREATE_API(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER
    ),
    MANAGE_SUBSCRIPTIONS(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER
    ),
    VIEW_SUBSCRIPTIONS(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER,
            OrganizationRole.VIEWER
    ),
    CREATE_API_KEY(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER
    ),
    VIEW_API_KEYS(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER,
            OrganizationRole.VIEWER
    ),
    REVOKE_API_KEY(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN
    ),
    MANAGE_API_KEY_POLICY(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.DEVELOPER
    );

    private final Set<OrganizationRole> allowedRoles;

    OrganizationPermission(OrganizationRole... allowedRoles) {
        this.allowedRoles = Set.of(allowedRoles);
    }

    public boolean isAllowedFor(OrganizationRole role) {
        return allowedRoles.contains(role);
    }
}
