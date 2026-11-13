package com.gateflow.management_api.subscription.exception;

public class InvalidSubscriptionStateException extends RuntimeException {

    public InvalidSubscriptionStateException(String message) {
        super(message);
    }
}
