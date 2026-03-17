package com.migros.couriertracking.controller;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import com.migros.couriertracking.constant.examples.LocationExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Courier Tracking", description = "APIs for courier geolocation tracking and distance calculation")
public interface CourierTrackingApi {

    @Operation(
            summary = "Report courier location",
            description = """
                    Accepts a courier geolocation update and updates total travel distance immediately.
                    Use Swagger's Authorize button with a token from /api/testing/example-token.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Location processed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or malformed Bearer token"),
            @ApiResponse(responseCode = "404", description = "Unknown courier"),
            @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    })
    ResponseEntity<String> reportLocation(
            @Parameter(hidden = true)
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Parameter(
                    description = "Optional idempotency key for duplicate POST protection",
                    example = "a4e23c4a-cf16-420e-8ff2-b534e0fcd997"
            )
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Courier location data",
                    content = @Content(examples = {
                            @ExampleObject(name = "1 - Near Atasehir Migros (entry)", summary = "Within 50m of store", value = LocationExamples.ATASEHIR_ENTRY),
                            @ExampleObject(name = "2 - Between Atasehir and Novada", summary = "Moving between two stores", value = LocationExamples.BETWEEN_STORES),
                            @ExampleObject(name = "3 - Near Novada Migros (entry)", summary = "Within 50m of store", value = LocationExamples.NOVADA_ENTRY),
                            @ExampleObject(name = "4 - Novada re-entry (skip)", summary = "Re-entry within 60s — not logged", value = LocationExamples.NOVADA_REENTRY_SKIP),
                            @ExampleObject(name = "5 - Caddebostan Migros (courier 2)", summary = "Different courier, different store", value = LocationExamples.CADDEBOSTAN_ENTRY),
                            @ExampleObject(name = "6 - Far from all stores", summary = "Outside 100m, no proximity trigger", value = LocationExamples.FAR_FROM_STORES)
                    })
            )
            @Valid @RequestBody CourierLocationRequest request);

    @Operation(
            summary = "Get total travel distance",
            description = "Returns the total distance (in meters) traveled by the specified courier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distance returned successfully"),
            @ApiResponse(responseCode = "404", description = "Unknown courier")
    })
    ResponseEntity<TotalDistanceResponse> getTotalDistance(
            @Parameter(description = "Unique courier identifier", example = "1")
            @PathVariable Long courierId);
}
