package com.migros.couriertracking.controller;

import com.migros.couriertracking.auth.CourierTokenAuthService;
import com.migros.couriertracking.dto.CourierLocationRequest;
import com.migros.couriertracking.dto.TotalDistanceResponse;
import com.migros.couriertracking.service.CourierTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourierTrackingController implements CourierTrackingApi {

    private final CourierTrackingService courierTrackingService;
    private final CourierTokenAuthService courierTokenAuthService;

    @Override
    @PostMapping("/courier-locations")
    public ResponseEntity<String> reportLocation(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CourierLocationRequest request) {
        Long courierId = courierTokenAuthService.resolveCourierId(authorizationHeader);
        courierTrackingService.processLocationUpdate(courierId, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body("Location processed successfully.");
    }

    @Override
    @GetMapping("/couriers/{courierId}/total-distance")
    public ResponseEntity<TotalDistanceResponse> getTotalDistance(
            @PathVariable Long courierId) {
        return ResponseEntity.ok(courierTrackingService.getTotalTravelDistance(courierId));
    }
}
