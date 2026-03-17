package com.migros.couriertracking.testdata;

import com.migros.couriertracking.dto.CourierLocationRequest;

import java.time.Instant;
import java.util.List;

/**
 * Test scenarios for Swagger / integration testing.
 * Scenarios:
 * 1. Courier-1: Enter Atasehir Migros → move → enter Novada Migros → distance query
 * 2. Courier-2: Move without approaching any store → distance query
 * 3. Courier-3: Re-enter the same store shortly after (reentry threshold test)
 * 4. Courier-1: Caddebostan → Ortakoy → Beylikduzu (3-store tour)
 */
public final class TestScenarios {

    private TestScenarios() {
    }

    // ──────────────────────────────────────────────
    // Scenario 1: Courier-1 — Two store entries
    // Ataşehir Migros (40.9923307, 29.1244229)
    // Novada Migros   (40.986106,  29.1161293)
    // ──────────────────────────────────────────────
    public static List<CourierLocationRequest> scenario1_twoStoreEntries() {
        Instant base = Instant.parse("2026-03-15T10:00:00Z");
        return List.of(
                // Step 1: Exactly on Atasehir Migros (~0m) → store entry expected
                new CourierLocationRequest(base, 40.9923307, 29.1244229),

                // Step 2: Move 500m north (moved away from store)
                new CourierLocationRequest(base.plusSeconds(120), 40.9968, 29.1244),

                // Step 3: Approach Novada Migros (~30m) → store entry expected
                new CourierLocationRequest(base.plusSeconds(300), 40.9863, 29.1163),

                // Step 4: Move 200m east
                new CourierLocationRequest(base.plusSeconds(420), 40.9863, 29.1190)
        );
    }

    // ──────────────────────────────────────────────
    // Scenario 2: Courier-2 — Movement without approaching any store
    // Along Kadikoy coastline (~1.5km route)
    // ──────────────────────────────────────────────
    public static List<CourierLocationRequest> scenario2_noStoreEntry() {
        Instant base = Instant.parse("2026-03-15T11:00:00Z");
        return List.of(
                // Kadikoy coastline start
                new CourierLocationRequest(base, 40.9900, 29.0250),

                // 500m south
                new CourierLocationRequest(base.plusSeconds(180), 40.9855, 29.0250),

                // 500m southeast
                new CourierLocationRequest(base.plusSeconds(360), 40.9810, 29.0300),

                // 500m east
                new CourierLocationRequest(base.plusSeconds(540), 40.9810, 29.0360)
        );
    }

    // ──────────────────────────────────────────────
    // Scenario 3: Courier-3 — Reentry threshold test
    // 2 entries to Atasehir Migros:
    //   - First entry → log
    //   - Re-enter 30s later → within threshold, SKIP
    //   - Re-enter 90s later → outside threshold, log
    // ──────────────────────────────────────────────
    public static List<CourierLocationRequest> scenario3_reentryThreshold() {
        Instant base = Instant.parse("2026-03-15T12:00:00Z");
        return List.of(
                // First entry: Atasehir Migros (~0m) → store entry
                new CourierLocationRequest(base, 40.9923, 29.1244),

                // Move away after 30s (200m north)
                new CourierLocationRequest(base.plusSeconds(30), 40.9941, 29.1244),

                // Re-enter after 30s (~0m) → within 60s threshold, SKIP
                new CourierLocationRequest(base.plusSeconds(50), 40.9923, 29.1244),

                // Move away
                new CourierLocationRequest(base.plusSeconds(70), 40.9941, 29.1244),

                // Re-enter after 90s (120s total) → outside threshold, store entry
                new CourierLocationRequest(base.plusSeconds(120), 40.9923, 29.1244)
        );
    }

    // ──────────────────────────────────────────────
    // Scenario 4: Courier-1 — Three-store tour (long route)
    // Caddebostan → Ortaköy → Beylikdüzü
    // ──────────────────────────────────────────────
    public static List<CourierLocationRequest> scenario4_threeStoreTour() {
        Instant base = Instant.parse("2026-03-15T14:00:00Z");
        return List.of(
                // Caddebostan MMM Migros (~50m) → store entry
                new CourierLocationRequest(base, 40.9636, 29.0633),

                // Intermediate location (Bostanci)
                new CourierLocationRequest(base.plusSeconds(600), 40.9680, 29.0900),

                // Ortaköy MMM Migros (~40m) → store entry
                new CourierLocationRequest(base.plusSeconds(1800), 41.0559, 29.0212),

                // Intermediate location (Levent)
                new CourierLocationRequest(base.plusSeconds(2400), 41.0800, 29.0100),

                // Beylikdüzü 5M Migros (~60m) → store entry
                new CourierLocationRequest(base.plusSeconds(3600), 41.0070, 28.6555),

                // Final location (outside Beylikduzu)
                new CourierLocationRequest(base.plusSeconds(4200), 41.0100, 28.6600)
        );
    }
}
