package com.migros.couriertracking.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standardized API error response")
public record ErrorResponse(
        @Schema(description = "Timestamp of the error", example = "2024-01-15T10:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error description", example = "Latitude must be between -90 and 90")
        String message
) {
}
