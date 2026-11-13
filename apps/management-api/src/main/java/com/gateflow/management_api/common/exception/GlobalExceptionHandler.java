package com.gateflow.management_api.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gateflow.management_api.api.exception.ApiAlreadyExistsException;
import com.gateflow.management_api.api.exception.ApiNotFoundException;
import com.gateflow.management_api.application.exception.ApplicationAlreadyExistsException;
import com.gateflow.management_api.application.exception.ApplicationNotFoundException;
import com.gateflow.management_api.common.response.ApiErrorResponse;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;
import com.gateflow.management_api.subscription.exception.InvalidSubscriptionStateException;
import com.gateflow.management_api.subscription.exception.SubscriptionAlreadyExistsException;
import com.gateflow.management_api.subscription.exception.SubscriptionNotAllowedException;
import com.gateflow.management_api.subscription.exception.SubscriptionNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleOrganizationAlreadyExists(
            OrganizationAlreadyExistsException exception
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrganizationNotFound(
            OrganizationNotFoundException exception
    ) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ApiAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleApiAlreadyExists(
            ApiAlreadyExistsException exception
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleApiNotFound(
            ApiNotFoundException exception
    ) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationNotFound(
            ApplicationNotFoundException exception
    ) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ApplicationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationAlreadyExists(
            ApplicationAlreadyExistsException exception) {

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(SubscriptionAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleSubscriptionAlreadyExists(
            SubscriptionAlreadyExistsException exception
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidSubscriptionStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSubscriptionState(
            InvalidSubscriptionStateException exception) {

        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(SubscriptionNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleSubscriptionNotAllowed(
            SubscriptionNotAllowedException exception
    ) {
        return buildError(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSubscriptionNotFound(
            SubscriptionNotFoundException exception) {

        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String message
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now()
        );

        return ResponseEntity.status(status).body(response);
    }
}
