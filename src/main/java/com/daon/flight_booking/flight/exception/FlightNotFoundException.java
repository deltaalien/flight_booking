package com.daon.flight_booking.flight.exception;

public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(Long id) {
        super("Flight not found: " + id);
    }
}
