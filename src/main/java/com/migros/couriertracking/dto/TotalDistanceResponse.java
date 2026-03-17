package com.migros.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Total distance traveled by a courier")
public record TotalDistanceResponse(
        @Schema(description = "Unique courier identifier", example = "1")
        Long courierId,

        @Schema(description = "Courier's full name", example = "Ahmet Yılmaz")
        String courierName,

        @Schema(description = "Total accumulated distance", example = "1523.45")
        double totalDistance,

        @Schema(description = "Unit of measurement", example = "meters")
        String unit
) {
}
