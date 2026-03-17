package com.migros.couriertracking.service;

import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;

public interface CourierTrackingService {

    void processLocationUpdate(Long courierId, CourierLocationRequest request, String idempotencyKey);

    TotalDistanceResponse getTotalTravelDistance(Long courierId);
}