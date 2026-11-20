package com.gateflow.management_api.application.exception;

public class ApplicationInactiveException extends RuntimeException {

    public ApplicationInactiveException(String message) {
        super(message);
    }
}
