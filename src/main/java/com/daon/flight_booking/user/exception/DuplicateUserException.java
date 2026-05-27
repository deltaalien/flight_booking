package com.daon.flight_booking.user.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String email) {
        super("User already registered: " + email);
    }
}
