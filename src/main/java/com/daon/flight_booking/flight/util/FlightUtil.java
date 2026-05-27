package com.daon.flight_booking.flight.util;

import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.shared.TimeUtil;

public final class FlightUtil {

    private FlightUtil() {
    }

    public static FlightResponse toResponse(Flight flight) {
        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(TimeUtil.toLocalDateTime(flight.getDepartureTime(), flight.getTimezone()))
                .timezone(flight.getTimezone())
                .seatRows(flight.getSeatRows())
                .seatColumns(flight.getSeatColumns())
                .openForBookingUntil(TimeUtil.toLocalDateTime(flight.getOpenForBookingUntil(), flight.getTimezone()))
                .build();
    }
}
