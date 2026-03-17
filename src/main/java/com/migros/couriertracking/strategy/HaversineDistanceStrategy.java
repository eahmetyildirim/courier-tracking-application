package com.migros.couriertracking.strategy;

import com.migros.couriertracking.model.Location;
import org.springframework.stereotype.Component;

import static com.migros.couriertracking.constant.Constants.EARTH_RADIUS_METERS;

/**
 * Haversine formula implementation for calculating the great-circle distance
 * between two points on a sphere given their longitudes and latitudes.
 * This is the most accurate method for real-world geographic distances.
 */
@Component
public class HaversineDistanceStrategy implements DistanceStrategy {

    @Override
    public double calculate(Location from, Location to) {
        double dLat = Math.toRadians(to.lat() - from.lat());
        double dLng = Math.toRadians(to.lng() - from.lng());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(from.lat())) * Math.cos(Math.toRadians(to.lat()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.HAVERSINE;
    }
}
