package com.migros.couriertracking.model;

import java.time.Instant;

public record CourierLocation(Long courierId, Location location, Instant time) {

    public CourierLocation(Long courierId, double lat, double lng, Instant time) {
        this(courierId, new Location(lat, lng), time);
    }

    public double lat() {
        return location.lat();
    }

    public double lng() {
        return location.lng();
    }

    public boolean isOutOfOrderComparedTo(CourierLocation other) {
        return !time.isAfter(other.time);
    }
}
