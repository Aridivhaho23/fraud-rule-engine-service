package com.example.fraud.exception;

/** Thrown when a request clashes with existing data (duplicate reference, already blacklisted...). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
