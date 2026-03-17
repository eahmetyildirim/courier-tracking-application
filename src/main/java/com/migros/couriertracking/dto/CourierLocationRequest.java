package com.migros.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for reporting a courier's geolocation update.
 *
 * <p>Uses boxed {@link Double} for lat/lng to enable {@code @NotNull} validation,
 * since primitive {@code double} cannot be null.</p>
 */
@Schema(description = "Courier location update request")
public record CourierLocationRequest(

        @NotNull(message = "Timestamp is required")
        @Schema(description = "Timestamp of the location update", example = "2024-01-15T10:00:00Z")
        Instant time,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        @Schema(description = "Latitude coordinate", example = "40.9923307")
        Double lat,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        @Schema(description = "Longitude coordinate", example = "29.1244229")
        Double lng
) {
    @AssertTrue(message = "Latitude must be a finite number")
    public boolean isLatitudeFinite() {
        return lat == null || Double.isFinite(lat);
    }

    @AssertTrue(message = "Longitude must be a finite number")
    public boolean isLongitudeFinite() {
        return lng == null || Double.isFinite(lng);
    }
}
