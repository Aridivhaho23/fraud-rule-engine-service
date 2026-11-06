package com.example.fraud.exception;

/** Thrown when a requested record does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " '" + identifier + "' was not found");
    }
}
