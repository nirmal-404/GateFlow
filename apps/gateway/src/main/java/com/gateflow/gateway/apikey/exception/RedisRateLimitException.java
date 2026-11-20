package com.gateflow.gateway.apikey.exception;

public class RedisRateLimitException extends RuntimeException {

    public RedisRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
