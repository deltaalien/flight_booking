package com.daon.flight_booking.user.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String user) {
        super("User already registered: " + user);
    }
}
