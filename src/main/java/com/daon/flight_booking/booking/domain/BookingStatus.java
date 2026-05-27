package com.daon.flight_booking.booking.domain;

import java.util.Set;

public enum BookingStatus {
    CANCELLED(), CONFIRMED(CANCELLED), HELD(CONFIRMED, CANCELLED);

    private final Set<BookingStatus> validStateTransitions;

    BookingStatus(BookingStatus ...statuses) {
        validStateTransitions = Set.of(statuses);
    }

    public boolean canTransitionTo(BookingStatus next) {
        return validStateTransitions.contains(next);
    }

}
