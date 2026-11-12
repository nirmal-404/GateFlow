package com.gateflow.management_api.organization.exception;

public class OrganizationAlreadyExistsException extends RuntimeException {

    public OrganizationAlreadyExistsException(String message) {
        super(message);
    }
}
