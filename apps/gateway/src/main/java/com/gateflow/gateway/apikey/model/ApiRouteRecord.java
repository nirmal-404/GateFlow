package com.gateflow.gateway.apikey.model;

import java.util.UUID;

public record ApiRouteRecord(
        UUID apiId,
        UUID organizationId,
        String slug,
        String version,
        String baseUrl
        ) {

}
