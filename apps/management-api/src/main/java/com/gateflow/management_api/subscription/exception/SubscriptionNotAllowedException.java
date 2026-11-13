package com.gateflow.management_api.subscription.exception;

public class SubscriptionNotAllowedException extends RuntimeException {

    public SubscriptionNotAllowedException(String message) {
        super(message);
    }
}
