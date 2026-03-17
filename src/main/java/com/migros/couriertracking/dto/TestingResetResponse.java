package com.migros.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Testing helper response after clearing in-memory runtime state")
public record TestingResetResponse(

        @Schema(description = "Human-readable reset result", example = "In-memory runtime state cleared.")
        String message
) {
}
