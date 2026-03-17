package com.migros.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Testing helper response containing recorded store-entry history for one courier")
public record StoreEntryHistoryResponse(

        @Schema(description = "Courier id whose store-entry history is returned", example = "1")
        Long courierId,

        @Schema(description = "Number of recorded store-entry events", example = "2")
        int entryCount,

        @Schema(description = "Recorded store-entry events ordered by time")
        List<Entry> entries
) {

    public record Entry(
            @Schema(description = "Store name", example = "Ataşehir MMM Migros")
            String storeName,

            @Schema(description = "Recorded entry timestamp", example = "2026-03-17T10:00:00Z")
            Instant time
    ) {
    }
}
