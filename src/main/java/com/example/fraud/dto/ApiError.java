package com.example.fraud.dto;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * The ONE JSON shape used for every error, so API clients handle failures predictably.
 *
 * @param violations per-field validation problems (empty for other kinds of error)
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldViolation> violations) {

    /** One invalid field in a request body. */
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldViolation> violations) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, violations);
    }
}
