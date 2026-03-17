package com.migros.couriertracking.strategy;

import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.model.Location;
import com.migros.couriertracking.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HaversineDistanceStrategyTest {

    private final HaversineDistanceStrategy strategy = new HaversineDistanceStrategy();

    @Test
    @DisplayName("Same point should return 0 distance")
    void samePointReturnsZero() {
        Location point = new Location(40.9923307, 29.1244229);
        double distance = strategy.calculate(point, point);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("Known distance between two Istanbul locations")
    void knownDistanceBetweenStores() {
        // Ataşehir MMM Migros -> Novada MMM Migros
        Store atasehir = new Store("Ataşehir", 40.9923307, 29.1244229);
        Store novada = new Store("Novada", 40.986106, 29.1161293);
        double distance = strategy.calculate(atasehir.location(), novada.location());
        // Approximately ~1km apart
        assertTrue(distance > 500 && distance < 2000,
                "Expected distance between 500m-2000m, got: " + distance);
    }

    @Test
    @DisplayName("Point within 100m should be detected")
    void pointWithin100Meters() {
        // Slightly offset from Ataşehir MMM Migros (within ~50m)
        Store store = new Store("Ataşehir", 40.9923307, 29.1244229);
        CourierLocation courier = new CourierLocation(1L, 40.9924, 29.1245, Instant.now());
        double distance = strategy.calculate(courier.location(), store.location());
        assertTrue(distance < 100, "Expected < 100m, got: " + distance);
    }

    @Test
    @DisplayName("Point outside 100m should be detected")
    void pointOutside100Meters() {
        // Further offset from Ataşehir MMM Migros
        Store store = new Store("Ataşehir", 40.9923307, 29.1244229);
        CourierLocation courier = new CourierLocation(1L, 40.994, 29.126, Instant.now());
        double distance = strategy.calculate(courier.location(), store.location());
        assertTrue(distance > 100, "Expected > 100m, got: " + distance);
    }

    @Test
    @DisplayName("Algorithm name should be Haversine")
    void algorithmName() {
        assertEquals(StrategyType.HAVERSINE, strategy.getStrategyType());
    }
}
