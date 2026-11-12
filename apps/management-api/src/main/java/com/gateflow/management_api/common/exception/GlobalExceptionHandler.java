package com.gateflow.management_api.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gateflow.management_api.api.exception.ApiAlreadyExistsException;
import com.gateflow.management_api.common.response.ApiErrorResponse;
import com.gateflow.management_api.organization.exception.OrganizationAlreadyExistsException;
import com.gateflow.management_api.organization.exception.OrganizationNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleOrganizationAlreadyExists(
            OrganizationAlreadyExistsException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrganizationNotFound(
            OrganizationNotFoundException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(ApiAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleApiAlreadyExists(
            ApiAlreadyExistsException exception
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
}
