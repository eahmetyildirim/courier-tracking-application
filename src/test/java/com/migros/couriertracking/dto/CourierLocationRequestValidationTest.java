package com.migros.couriertracking.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierLocationRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Latitude and longitude should reject out-of-range values")
    void shouldRejectOutOfRangeCoordinates() {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 91.0, -181.0
        );

        Set<ConstraintViolation<CourierLocationRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Latitude must be between -90 and 90")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Longitude must be between -180 and 180")));
    }

    @Test
    @DisplayName("Latitude and longitude should reject NaN and infinity")
    void shouldRejectNonFiniteCoordinates() {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), Double.NaN, Double.POSITIVE_INFINITY
        );

        Set<ConstraintViolation<CourierLocationRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Latitude must be a finite number")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Longitude must be a finite number")));
    }

    @Test
    @DisplayName("Valid coordinates should pass validation")
    void shouldAcceptValidCoordinates() {
        CourierLocationRequest request = new CourierLocationRequest(
                Instant.now(), 40.9923307, 29.1244229
        );

        Set<ConstraintViolation<CourierLocationRequest>> violations = validator.validate(request);

        assertFalse(violations.iterator().hasNext());
    }
}
