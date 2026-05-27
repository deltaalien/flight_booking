package com.daon.flight_booking.flight.dto;

import com.daon.flight_booking.flight.domain.Airport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private Airport origin;
    private Airport destination;
    private LocalDateTime departureTime;
    private String timezone;
    private int seatRows;
    private int seatColumns;
    private LocalDateTime openForBookingUntil;
}
