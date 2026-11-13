package com.gateflow.management_api.subscription.exception;

public class SubscriptionAlreadyExistsException extends RuntimeException {

    public SubscriptionAlreadyExistsException(String message) {
        super(message);
    }
}
