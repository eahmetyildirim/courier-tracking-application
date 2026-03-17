package com.migros.couriertracking.strategy;

import com.migros.couriertracking.model.Location;
import org.springframework.stereotype.Component;

import static com.migros.couriertracking.constant.Constants.METERS_PER_DEGREE_LAT;

/**
 * Simplified Euclidean approximation for distance calculation.
 * Less accurate than Haversine but faster for very short distances.
 * Converts lat/lng differences to approximate meters using equirectangular projection.
 */
@Component
public class EuclideanDistanceStrategy implements DistanceStrategy {

    @Override
    public double calculate(Location from, Location to) {
        double avgLat = Math.toRadians((from.lat() + to.lat()) / 2);

        double dLat = (to.lat() - from.lat()) * METERS_PER_DEGREE_LAT;
        double dLng = (to.lng() - from.lng()) * METERS_PER_DEGREE_LAT * Math.cos(avgLat);

        return Math.sqrt(dLat * dLat + dLng * dLng);
    }

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.EUCLIDEAN;
    }
}
