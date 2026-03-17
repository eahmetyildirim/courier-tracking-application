package com.migros.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Testing helper response for generating a lightweight Bearer token")
public record ExampleBearerTokenResponse(

        @Schema(description = "Courier id encoded into the token payload", example = "1")
        Long courierId,

        @Schema(description = "Whether this courier exists in couriers.json", example = "true")
        boolean registered,

        @Schema(description = "Courier display name when registered", example = "Ahmet Yılmaz", nullable = true)
        String courierName,

        @Schema(description = "Raw token value to paste into Swagger Authorize", example = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJjb3VyaWVySWQiOjF9.signature")
        String token,

        @Schema(description = "Convenience header value for Postman or curl", example = "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJjb3VyaWVySWQiOjF9.signature")
        String authorizationHeader,

        @Schema(description = "Short usage note for manual testing", example = "Paste the token field into Swagger Authorize. Use authorizationHeader directly in Postman or curl.")
        String usageHint
) {
}
