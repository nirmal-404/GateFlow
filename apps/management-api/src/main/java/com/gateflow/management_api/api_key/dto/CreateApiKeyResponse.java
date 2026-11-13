package com.gateflow.management_api.api_key.dto;

public record CreateApiKeyResponse(
        ApiKeyResponse apiKey,
        String secretKey
        ) {

}
