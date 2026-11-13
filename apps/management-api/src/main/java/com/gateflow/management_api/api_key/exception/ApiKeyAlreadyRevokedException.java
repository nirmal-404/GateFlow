package com.gateflow.management_api.api_key.exception;

public class ApiKeyAlreadyRevokedException extends RuntimeException {

    public ApiKeyAlreadyRevokedException(String message) {
        super(message);
    }
}
