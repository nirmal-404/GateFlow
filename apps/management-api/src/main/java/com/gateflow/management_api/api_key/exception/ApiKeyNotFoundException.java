package com.gateflow.management_api.api_key.exception;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(String message) {
        super(message);
    }
}
