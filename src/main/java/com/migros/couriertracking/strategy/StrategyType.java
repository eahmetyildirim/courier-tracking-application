package com.migros.couriertracking.strategy;

import java.util.Arrays;

public enum StrategyType {
    HAVERSINE,
    EUCLIDEAN;

    /**
     * Resolve a strategy type from its name with a descriptive error message on failure.
     *
     * @param name the strategy name from configuration
     * @return the matching {@link StrategyType}
     * @throws IllegalArgumentException if the name does not match any known strategy
     */
    public static StrategyType fromName(String name) {
        try {
            return StrategyType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid distance strategy '%s'. Supported values: %s",
                            name, Arrays.toString(values())),
                    e);
        }
    }
}