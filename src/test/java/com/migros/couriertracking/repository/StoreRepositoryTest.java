package com.migros.couriertracking.repository;

import com.migros.couriertracking.model.Location;
import com.migros.couriertracking.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreRepositoryTest {

    private StoreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new StoreRepository();
        repository.init();
    }

    @Test
    @DisplayName("findNearby returns store when querying its exact coordinates")
    void findNearbyReturnsStoreAtExactCoordinates() {
        // Ataşehir MMM Migros exact coordinates
        List<Store> nearby = repository.findNearby(locationAt(40.9923307, 29.1244229));
        assertTrue(nearby.stream().anyMatch(s -> s.name().contains("Ataşehir")),
                "Should find Ataşehir store at its exact coordinates");
    }

    @Test
    @DisplayName("findNearby returns store when querying ~50m away")
    void findNearbyReturnsStoreWhenClose() {
        // ~50m north of Ataşehir MMM Migros
        List<Store> nearby = repository.findNearby(locationAt(40.9927, 29.1244));
        assertTrue(nearby.stream().anyMatch(s -> s.name().contains("Ataşehir")),
                "Should find Ataşehir store when 50m away");
    }

    @Test
    @DisplayName("findNearby returns empty list for distant coordinates")
    void findNearbyReturnsEmptyForDistantLocation() {
        // Ankara coordinates - far from any Istanbul store
        List<Store> nearby = repository.findNearby(locationAt(39.9334, 32.8597));
        assertTrue(nearby.isEmpty(), "Should return empty list for Ankara coordinates");
    }

    @Test
    @DisplayName("Each known store is findable at its own coordinates")
    void eachStoreIsIndexed() {
        // All 5 stores from stores.json should be findable via findNearby at their coordinates
        assertFalse(repository.findNearby(locationAt(40.9923307, 29.1244229)).isEmpty(), "Ataşehir store should be findable");
        assertFalse(repository.findNearby(locationAt(40.986106, 29.1161293)).isEmpty(), "Novada store should be findable");
        assertFalse(repository.findNearby(locationAt(40.9632463, 29.0630908)).isEmpty(), "Caddebostan store should be findable");
        assertFalse(repository.findNearby(locationAt(41.0559546, 29.0214517)).isEmpty(), "Ortaköy store should be findable");
        assertFalse(repository.findNearby(locationAt(41.0066851, 28.6552262)).isEmpty(), "Beylikdüzü store should be findable");
    }

    @Test
    @DisplayName("findNearby does not return stores in different neighborhoods")
    void findNearbyDoesNotReturnDistantStores() {
        // Coordinates near Ataşehir should NOT return Beylikdüzü (~40km away)
        List<Store> nearby = repository.findNearby(locationAt(40.9923307, 29.1244229));
        assertFalse(nearby.stream().anyMatch(s -> s.name().contains("Beylikdüzü")),
                "Beylikdüzü should not appear in Ataşehir neighborhood");
    }

    private static Location locationAt(double lat, double lng) {
        return new Location(lat, lng);
    }
}
