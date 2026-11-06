package com.example.fraud.exception;

/** Thrown when a business rule forbids the request (e.g. an illegal flag status change). Mapped to HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
