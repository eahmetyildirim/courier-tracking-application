package com.migros.couriertracking.strategy;

import com.migros.couriertracking.model.Location;

/**
 * Strategy Pattern: Distance calculation strategy interface.
 * Allows switching between different distance calculation algorithms.
 * Accepts {@link Location} objects for type-safe, domain-driven distance computation.
 */
public interface DistanceStrategy {

    /**
     * Calculate the distance between two geographic locations.
     *
     * @param from the starting location
     * @param to   the destination location
     * @return distance in meters
     */
    double calculate(Location from, Location to);

    StrategyType getStrategyType();
}
