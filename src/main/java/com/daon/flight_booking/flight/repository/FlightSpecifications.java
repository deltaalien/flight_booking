package com.daon.flight_booking.flight.repository;

import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class FlightSpecifications {

    private FlightSpecifications() {
    }

    public static Specification<Flight> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }

    public static Specification<Flight> hasOrigin(Airport origin) {
        return (root, query, criteriaBuilder) ->
                origin == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("origin"), origin);
    }

    public static Specification<Flight> hasDestination(Airport destination) {
        return (root, query, criteriaBuilder) ->
                destination == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("destination"), destination);
    }

    public static Specification<Flight> departingOnDate(LocalDate date, String timezone) {
        if (date == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneOffset.UTC;

        OffsetDateTime start = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(criteriaBuilder.greaterThanOrEqualTo(root.get("departureTime"), start),
                        criteriaBuilder.lessThan(root.get("departureTime"), end));
    }
}
