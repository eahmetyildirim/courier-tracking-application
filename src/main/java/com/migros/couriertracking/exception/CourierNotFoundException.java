package com.migros.couriertracking.exception;

/**
 * Thrown when a courier ID is not found in the registered courier list.
 */
public class CourierNotFoundException extends RuntimeException {

    public CourierNotFoundException(Long courierId) {
        super("Courier not found with id: " + courierId);
    }
}
