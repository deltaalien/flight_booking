package com.daon.flight_booking.booking.exception;

public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String seat) {
        super("Seat " + seat + " is not available");
    }

    public SeatUnavailableException(int row, int col) {
        super("Seat at row " + row + ", column " + col + " is not available");
    }
}
