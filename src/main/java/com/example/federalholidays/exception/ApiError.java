package com.example.federalholidays.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "API error response")
public record ApiError(
        @Schema(description = "Error timestamp", example = "2026-07-01T12:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error message", example = "Validation failed")
        String message,

        @Schema(description = "Detailed validation or processing errors")
        List<String> details
) {
    static ApiError of(org.springframework.http.HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, List.of());
    }

    static ApiError of(org.springframework.http.HttpStatus status, String message, List<String> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details);
    }
}
