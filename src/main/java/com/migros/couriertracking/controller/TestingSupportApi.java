package com.migros.couriertracking.controller;

import com.migros.couriertracking.dto.ExampleBearerTokenResponse;
import com.migros.couriertracking.dto.StoreEntryHistoryResponse;
import com.migros.couriertracking.dto.TestingResetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Testing Helpers", description = "Local testing helpers for Swagger and Postman")
public interface TestingSupportApi {

    @Operation(
            summary = "Generate example Bearer token",
            description = """
                    Creates a lightweight JWT-like Bearer token for local testing.
                    The application only decodes the payload and reads the courierId claim.
                    Use the returned raw token in Swagger Authorize, or use authorizationHeader directly in Postman/curl.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Example token generated",
            content = @Content(schema = @Schema(implementation = ExampleBearerTokenResponse.class), examples = {
                    @ExampleObject(
                            name = "Registered courier token",
                            value = """
                                    {
                                      "courierId": 1,
                                      "registered": true,
                                      "courierName": "Ahmet Yılmaz",
                                      "token": "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJjb3VyaWVySWQiOjF9.signature",
                                      "authorizationHeader": "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJjb3VyaWVySWQiOjF9.signature",
                                      "usageHint": "Paste the token field into Swagger Authorize. Use authorizationHeader directly in Postman or curl."
                                    }
                                    """
                    )
            })
    )
    ResponseEntity<ExampleBearerTokenResponse> generateExampleToken(
            @Parameter(description = "Courier id to encode in the token payload", example = "1")
            @RequestParam(defaultValue = "1") Long courierId);

    @Operation(
            summary = "Read store-entry history",
            description = "Returns the in-memory store-entry records captured for one courier during the current app lifetime."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Store-entry history returned",
            content = @Content(schema = @Schema(implementation = StoreEntryHistoryResponse.class))
    )
    ResponseEntity<StoreEntryHistoryResponse> getStoreEntryHistory(
            @Parameter(description = "Courier id whose recorded store-entry events will be returned", example = "1")
            @RequestParam Long courierId);

    @Operation(
            summary = "Reset in-memory runtime state",
            description = "Clears in-memory distance state, store-entry cache/history, and idempotency reservations for isolated manual testing."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Runtime state cleared",
            content = @Content(schema = @Schema(implementation = TestingResetResponse.class))
    )
    ResponseEntity<TestingResetResponse> resetState();
}
