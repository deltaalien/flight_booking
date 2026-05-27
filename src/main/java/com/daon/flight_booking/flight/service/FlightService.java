package com.daon.flight_booking.flight.service;

import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.flight.repository.FlightSpecifications;
import com.daon.flight_booking.flight.util.FlightUtil;
import com.daon.flight_booking.shared.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    static final String CACHE_NAME = "flights";

    private final FlightRepository flightRepository;

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
                .map(FlightUtil::toResponse)
                .toList();
    }

    public Optional<Flight> getFlightById(long id) {
        return flightRepository.findById(id);
    }
}
