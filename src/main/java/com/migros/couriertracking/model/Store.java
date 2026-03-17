package com.migros.couriertracking.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Store(String name, Location location) {

    @JsonCreator
    public Store(
            @JsonProperty("name") String name,
            @JsonProperty("lat") double lat,
            @JsonProperty("lng") double lng
    ) {
        this(name, new Location(lat, lng));
    }

    public double lat() {
        return location.lat();
    }

    public double lng() {
        return location.lng();
    }
}
