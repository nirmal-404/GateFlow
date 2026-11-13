package com.gateflow.management_api.api_key_policy.exception;

public class ApiKeyPolicyAlreadyExistsException extends RuntimeException {

    public ApiKeyPolicyAlreadyExistsException(String message) {
        super(message);
    }
}
