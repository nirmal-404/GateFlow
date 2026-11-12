package com.gateflow.management_api.api.exception;

public class ApiAlreadyExistsException extends RuntimeException {

    public ApiAlreadyExistsException(String message) {
        super(message);
    }
}
