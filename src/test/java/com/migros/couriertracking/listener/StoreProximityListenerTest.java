package com.migros.couriertracking.listener;

import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.model.Courier;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.repository.CourierStoreEntryRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.service.StoreService;
import com.migros.couriertracking.service.impl.StoreServiceImpl;
import com.migros.couriertracking.strategy.DistanceStrategyRegistry;
import com.migros.couriertracking.strategy.HaversineDistanceStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreProximityListenerTest {

    private static final Courier TEST_COURIER = new Courier(1L, "Test Courier");

    // Ataşehir MMM Migros coordinates
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    private StoreProximityListener listener;
    private StoreService storeService;
    private CourierStoreEntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        StoreRepository storeRepository = new StoreRepository();
        ReflectionTestUtils.invokeMethod(storeRepository, "init");

        entryRepository = new CourierStoreEntryRepository();
        ReflectionTestUtils.setField(entryRepository, "reentryThresholdSeconds", 60L);
        ReflectionTestUtils.invokeMethod(entryRepository, "init");

        StoreServiceImpl storeServiceImpl = new StoreServiceImpl(storeRepository, entryRepository);
        ReflectionTestUtils.setField(storeServiceImpl, "reentryThresholdSeconds", 60L);
        storeService = storeServiceImpl;

        DistanceStrategyRegistry registry = new DistanceStrategyRegistry(List.of(
                new HaversineDistanceStrategy()
        ));

        listener = new StoreProximityListener(storeService, registry, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(listener, "proximityMeters", 100.0);
        ReflectionTestUtils.setField(listener, "strategyTypeName", "HAVERSINE");
        ReflectionTestUtils.invokeMethod(listener, "init");
    }

    private CourierLocationEvent createEvent(double lat, double lng, Instant time) {
        return new CourierLocationEvent(this,
                new CourierLocation(TEST_COURIER.id(), lat, lng, time), TEST_COURIER);
    }

    @Test
    @DisplayName("Should record entry when courier is within 100m of a store")
    void shouldRecordEntryWithinProximity() {
        Instant now = Instant.now();

        // Exactly at Ataşehir Migros (0m distance)
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, now));

        assertTrue(storeService.isRecentEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros", now));
    }

    @Test
    @DisplayName("Should not record entry when courier is beyond 100m from all stores")
    void shouldNotRecordEntryBeyondProximity() {
        Instant now = Instant.now();

        // ~2km away from any store (Kadıköy sahil)
        listener.onLocationUpdate(createEvent(40.990, 29.025, now));

        assertFalse(storeService.isRecentEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros", now));
    }

    @Test
    @DisplayName("Should skip reentry within 1 minute threshold")
    void shouldSkipReentryWithinThreshold() {
        Instant now = Instant.now();

        // First entry at Ataşehir Migros
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, now));
        Instant firstEntryTime = entryRepository.getLastEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros").orElse(null);
        assertEquals(now, firstEntryTime, "First entry should be recorded");

        // Move away (500m north)
        listener.onLocationUpdate(createEvent(40.9968, 29.1244, now.plusSeconds(20)));

        // Come back at 30s — within 60s threshold, should not re-record
        Instant reentryTime = now.plusSeconds(30);
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, reentryTime));

        // Verify the entry timestamp is still the ORIGINAL time, not the reentry time.
        Instant storedTime = entryRepository.getLastEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros").orElse(null);
        assertEquals(now, storedTime,
                "Entry timestamp must remain the original time — reentry should NOT overwrite it");
    }

    @Test
    @DisplayName("Should allow reentry after 1 minute threshold expires")
    void shouldAllowReentryAfterThreshold() {
        Instant now = Instant.now();

        // First entry
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, now));

        // Move away
        listener.onLocationUpdate(createEvent(40.9968, 29.1244, now.plusSeconds(30)));

        // Come back after 61 seconds — beyond threshold, should record new entry
        Instant reentryTime = now.plusSeconds(61);
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, reentryTime));

        assertTrue(storeService.isRecentEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros", reentryTime));
    }

    @Test
    @DisplayName("Should track entries for different stores independently")
    void shouldTrackDifferentStoresIndependently() {
        Instant now = Instant.now();

        // Enter Ataşehir Migros
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT, ATASEHIR_LNG, now));

        // Enter Novada Migros (40.986106, 29.1161293)
        listener.onLocationUpdate(createEvent(40.986106, 29.1161293, now.plusSeconds(120)));

        // Both entries should exist
        assertTrue(entryRepository.getLastEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros").isPresent());
        assertTrue(entryRepository.getLastEntry(
                TEST_COURIER.id(), "Novada MMM Migros").isPresent());
    }

    @Test
    @DisplayName("Should record entry at boundary distance (~95m)")
    void shouldRecordEntryAtBoundary() {
        Instant now = Instant.now();

        // ~95m north of Ataşehir Migros (approximately 0.00085 degrees latitude)
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT + 0.00085, ATASEHIR_LNG, now));

        assertTrue(storeService.isRecentEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros", now));
    }

    @Test
    @DisplayName("Should not record entry just outside 100m (~110m)")
    void shouldNotRecordEntryJustOutside() {
        Instant now = Instant.now();

        // ~110m north of Ataşehir Migros (approximately 0.001 degrees latitude)
        listener.onLocationUpdate(createEvent(ATASEHIR_LAT + 0.001, ATASEHIR_LNG, now));

        assertFalse(storeService.isRecentEntry(
                TEST_COURIER.id(), "Ataşehir MMM Migros", now));
    }
}
