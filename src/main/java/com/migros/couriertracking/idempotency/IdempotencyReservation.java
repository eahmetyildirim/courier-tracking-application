package com.migros.couriertracking.idempotency;

public record IdempotencyReservation(String scopedKey, boolean tracked, boolean duplicate) {

    public static IdempotencyReservation untracked() {
        return new IdempotencyReservation("", false, false);
    }

    public static IdempotencyReservation duplicate(String scopedKey) {
        return new IdempotencyReservation(scopedKey, true, true);
    }

    public static IdempotencyReservation newRequest(String scopedKey) {
        return new IdempotencyReservation(scopedKey, true, false);
    }
}
