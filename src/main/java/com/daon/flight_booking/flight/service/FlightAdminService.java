package com.daon.flight_booking.flight.service;

import com.daon.flight_booking.booking.service.BookingService;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.CreateFlightRequest;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.exception.FlightNotFoundException;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.shared.BookingProperties;
import com.daon.flight_booking.shared.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static com.daon.flight_booking.flight.service.FlightService.CACHE_NAME;
import static com.daon.flight_booking.flight.util.FlightUtil.toResponse;

@Service
@RequiredArgsConstructor
public class FlightAdminService {

    private final FlightRepository flightRepository;
    private final BookingService bookingService;
    private final BookingProperties bookingProperties;

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public FlightResponse createFlight(CreateFlightRequest request) {

        OffsetDateTime departureTime = TimeUtil.toOffsetDateTime(request.getDepartureTime(), request.getTimezone());
        if (!departureTime.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Departure time must be in the future");
        }

        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureTime(departureTime)
                .timezone(request.getTimezone())
                .seatRows(request.getSeatRows())
                .seatColumns(request.getSeatColumns())
                .openForBookingUntil(departureTime.minusMinutes(bookingProperties.getCutoffMinutes()))
                .build();

        return toResponse(flightRepository.save(flight));
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteFlight(Long id) {

        Flight flight = flightRepository.findById(id).orElseThrow(() -> new FlightNotFoundException(id));

        int ignore = bookingService.cancelActiveBookingsForFlight(id);

        flight.setDeleted(true);
        flightRepository.save(flight);
    }
}
