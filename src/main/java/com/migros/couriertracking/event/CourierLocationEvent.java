package com.migros.couriertracking.event;

import com.migros.couriertracking.model.Courier;
import com.migros.couriertracking.model.CourierLocation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a courier's location is updated.
 * Carries the courier location data and the registered courier
 * information to all registered listeners.
 */
@Getter
public class CourierLocationEvent extends ApplicationEvent {

    private final CourierLocation location;
    private final Courier courier;

    public CourierLocationEvent(Object source, CourierLocation location, Courier courier) {
        super(source);
        this.location = location;
        this.courier = courier;
    }
}
