package com.daon.flight_booking.flight.service;

import com.daon.flight_booking.booking.service.BookingService;
import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.CreateFlightRequest;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.exception.FlightNotFoundException;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.flight.repository.FlightSpecifications;
import com.daon.flight_booking.shared.BookingProperties;
import com.daon.flight_booking.shared.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private static final String CACHE_NAME = "flights";

    private final FlightRepository flightRepository;
    private final BookingService bookingService;
    private final BookingProperties bookingProperties;

    private static FlightResponse toResponse(Flight flight) {
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

    @Cacheable(CACHE_NAME)
    public List<FlightResponse> getFlights(Airport origin, Airport destination, LocalDate date, String timezone) {

        if (timezone != null) {
            TimeUtil.validateTimezone(timezone);
        }

        Specification<Flight> spec = Specification
                .where(FlightSpecifications.notDeleted())
                .and(FlightSpecifications.hasOrigin(origin))
                .and(FlightSpecifications.hasDestination(destination))
                .and(FlightSpecifications.departingOnDate(date, timezone));

        return flightRepository.findAll(spec).stream()
                .map(FlightService::toResponse)
                .toList();
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
