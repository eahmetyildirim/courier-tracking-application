package com.migros.couriertracking.constant.examples;

public final class LocationExamples {

    private LocationExamples() {}

    public static final String ATASEHIR_ENTRY = """
            {
              "time": "2024-01-15T10:00:00Z",
              "lat": 40.9920,
              "lng": 29.1242
            }""";

    public static final String BETWEEN_STORES = """
            {
              "time": "2024-01-15T10:01:00Z",
              "lat": 40.9890,
              "lng": 29.1200
            }""";

    public static final String NOVADA_ENTRY = """
            {
              "time": "2024-01-15T10:02:00Z",
              "lat": 40.9863,
              "lng": 29.1163
            }""";

    public static final String NOVADA_REENTRY_SKIP = """
            {
              "time": "2024-01-15T10:02:30Z",
              "lat": 40.9860,
              "lng": 29.1160
            }""";

    public static final String CADDEBOSTAN_ENTRY = """
            {
              "time": "2024-01-15T10:00:00Z",
              "lat": 40.9634,
              "lng": 29.0632
            }""";

    public static final String FAR_FROM_STORES = """
            {
              "time": "2024-01-15T10:03:00Z",
              "lat": 41.0100,
              "lng": 29.0300
            }""";
}
