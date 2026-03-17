package com.migros.couriertracking.constant;

/**
 * Shared domain constants for the Courier Tracking service.
 * Contains values that are reusable across multiple classes.
 */
public final class Constants {

    private Constants() {
    }

    // ── Geographic ──────────────────────────────────────────────────────

    /** Earth's mean radius in meters (WGS-84). */
    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** Approximate meters per one degree of latitude. */
    public static final double METERS_PER_DEGREE_LAT = 111_320.0;

    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;

    // ── Configuration Property Keys ─────────────────────────────────────

    /** Distance calculation strategy, used by both listeners. */
    public static final String PROP_DISTANCE_STRATEGY = "${courier.distance.strategy:HAVERSINE}";
}
