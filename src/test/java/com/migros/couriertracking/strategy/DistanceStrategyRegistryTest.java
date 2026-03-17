package com.migros.couriertracking.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistanceStrategyRegistryTest {

    private DistanceStrategyRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DistanceStrategyRegistry(List.of(
                new HaversineDistanceStrategy(),
                new EuclideanDistanceStrategy()
        ));
    }

    @Test
    @DisplayName("Registry should return HaversineDistanceStrategy for HAVERSINE type")
    void getHaversineStrategy() {
        DistanceStrategy strategy = registry.getStrategy(StrategyType.HAVERSINE);

        assertInstanceOf(HaversineDistanceStrategy.class, strategy);
        assertEquals(StrategyType.HAVERSINE, strategy.getStrategyType());
    }

    @Test
    @DisplayName("Registry should return EuclideanDistanceStrategy for EUCLIDEAN type")
    void getEuclideanStrategy() {
        DistanceStrategy strategy = registry.getStrategy(StrategyType.EUCLIDEAN);

        assertInstanceOf(EuclideanDistanceStrategy.class, strategy);
        assertEquals(StrategyType.EUCLIDEAN, strategy.getStrategyType());
    }

    @Test
    @DisplayName("Registry should return the same singleton instance on repeated calls")
    void registryReturnsSameInstance() {
        DistanceStrategy first = registry.getStrategy(StrategyType.HAVERSINE);
        DistanceStrategy second = registry.getStrategy(StrategyType.HAVERSINE);

        assertSame(first, second);
    }
}
