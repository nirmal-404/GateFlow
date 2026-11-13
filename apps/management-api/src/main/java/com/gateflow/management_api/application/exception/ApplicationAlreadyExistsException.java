package com.gateflow.management_api.application.exception;

public class ApplicationAlreadyExistsException extends RuntimeException {

    public ApplicationAlreadyExistsException(String message) {
        super(message);
    }
}
