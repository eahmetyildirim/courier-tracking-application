package com.migros.couriertracking.listener;

import com.migros.couriertracking.event.CourierLocationEvent;
import com.migros.couriertracking.model.Courier;
import com.migros.couriertracking.model.CourierLocation;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.strategy.DistanceStrategyRegistry;
import com.migros.couriertracking.strategy.HaversineDistanceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistanceTrackingListenerTest {

    private DistanceTrackingListener listener;
    private CourierDistanceRepository repository;

    @BeforeEach
    void setUp() {
        DistanceStrategyRegistry registry = new DistanceStrategyRegistry(List.of(
                new HaversineDistanceStrategy()
        ));
        repository = new CourierDistanceRepository();
        listener = new DistanceTrackingListener(registry, repository);
        ReflectionTestUtils.setField(listener, "strategyTypeName", "HAVERSINE");
        listener.init();
    }

    private CourierLocationEvent createEvent(Long courierId, double lat, double lng, Instant time) {
        Courier courier = new Courier(courierId, "Test Courier " + courierId);
        return new CourierLocationEvent(this, new CourierLocation(courierId, lat, lng, time), courier);
    }

    @Test
    @DisplayName("First location should not add any distance")
    void firstLocationNoDistance() {
        listener.onLocationUpdate(createEvent(1L, 40.99, 29.12, Instant.now()));
        assertEquals(0.0, repository.getTotalDistance(1L));
    }

    @Test
    @DisplayName("Two locations should calculate distance between them")
    void twoLocationsShouldCalculateDistance() {
        Instant now = Instant.now();
        listener.onLocationUpdate(createEvent(1L, 40.99, 29.12, now));
        listener.onLocationUpdate(createEvent(1L, 40.991, 29.121, now.plusSeconds(10)));

        assertTrue(repository.getTotalDistance(1L) > 0);
    }

    @Test
    @DisplayName("Distance should accumulate over multiple locations")
    void distanceShouldAccumulate() {
        Instant now = Instant.now();
        listener.onLocationUpdate(createEvent(1L, 40.99, 29.12, now));
        listener.onLocationUpdate(createEvent(1L, 40.991, 29.121, now.plusSeconds(10)));

        double afterTwo = repository.getTotalDistance(1L);

        listener.onLocationUpdate(createEvent(1L, 40.992, 29.122, now.plusSeconds(20)));

        assertTrue(repository.getTotalDistance(1L) > afterTwo);
    }

    @Test
    @DisplayName("Unknown courier should return 0 distance")
    void unknownCourierReturnsZero() {
        assertEquals(0.0, repository.getTotalDistance(999L));
    }

    @Test
    @DisplayName("Different couriers tracked independently")
    void differentCouriersIndependent() {
        Instant now = Instant.now();
        listener.onLocationUpdate(createEvent(1L, 40.99, 29.12, now));
        listener.onLocationUpdate(createEvent(1L, 41.00, 29.13, now.plusSeconds(10)));

        listener.onLocationUpdate(createEvent(2L, 40.99, 29.12, now));
        listener.onLocationUpdate(createEvent(2L, 40.991, 29.121, now.plusSeconds(10)));

        assertTrue(repository.getTotalDistance(1L) > repository.getTotalDistance(2L));
    }

    @Test
    @DisplayName("Out-of-order location should be ignored")
    void outOfOrderLocationShouldBeIgnored() {
        Instant now = Instant.now();
        listener.onLocationUpdate(createEvent(1L, 40.99, 29.12, now));
        listener.onLocationUpdate(createEvent(1L, 40.991, 29.121, now.plusSeconds(20)));

        double distanceAfterSecondUpdate = repository.getTotalDistance(1L);

        listener.onLocationUpdate(createEvent(1L, 41.5, 30.5, now.plusSeconds(10)));

        assertEquals(distanceAfterSecondUpdate, repository.getTotalDistance(1L));
    }
}
