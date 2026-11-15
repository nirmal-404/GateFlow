package com.gateflow.management_api.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String fullName,
        String email,
        String status
        ) {

}
