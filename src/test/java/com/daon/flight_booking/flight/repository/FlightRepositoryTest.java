package com.daon.flight_booking.flight.repository;

import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.daon.flight_booking.flight.repository.FlightSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class FlightRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.contexts", () -> "test");
    }

    @Autowired
    FlightRepository flightRepository;

    @Test
    void notDeleted_excludesDeletedFlight() {
        Flight active = flightRepository.save(buildFlight(Airport.DUB, Airport.LHR,
                OffsetDateTime.now().plusDays(10)));
        Flight deleted = buildFlight(Airport.DUB, Airport.AMS, OffsetDateTime.now().plusDays(10));
        deleted.setDeleted(true);
        flightRepository.save(deleted);

        List<Flight> result = flightRepository.findAll(notDeleted());

        assertThat(result).extracting(Flight::getId).containsExactly(active.getId());
    }

    @Test
    void hasOrigin_filtersCorrectly() {
        flightRepository.save(buildFlight(Airport.DUB, Airport.LHR, OffsetDateTime.now().plusDays(10)));
        flightRepository.save(buildFlight(Airport.LHR, Airport.DUB, OffsetDateTime.now().plusDays(10)));

        List<Flight> result = flightRepository.findAll(
                Specification.where(notDeleted()).and(hasOrigin(Airport.DUB)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrigin()).isEqualTo(Airport.DUB);
    }

    @Test
    void departingOnDate_utc_returnsMatchingFlight() {
        OffsetDateTime departure = OffsetDateTime.parse("2030-06-01T10:00:00Z");
        flightRepository.save(buildFlight(Airport.DUB, Airport.LHR, departure));

        List<Flight> found = flightRepository.findAll(departingOnDate(LocalDate.of(2030, 6, 1), null));
        List<Flight> notFound = flightRepository.findAll(departingOnDate(LocalDate.of(2030, 5, 31), null));

        assertThat(found).hasSize(1);
        assertThat(notFound).isEmpty();
    }

    @Test
    void departingOnDate_withTimezone_interpretsDateInGivenZone() {
        // 2030-06-01T00:30:00+01:00 (Dublin) = 2030-05-31T23:30:00Z
        OffsetDateTime departure = OffsetDateTime.of(2030, 6, 1, 0, 30, 0, 0, ZoneOffset.ofHours(1));
        flightRepository.save(buildFlight(Airport.DUB, Airport.LHR, departure));

        List<Flight> foundInDublin = flightRepository.findAll(
                departingOnDate(LocalDate.of(2030, 6, 1), "Europe/Dublin"));

        List<Flight> notFoundInUtc = flightRepository.findAll(
                departingOnDate(LocalDate.of(2030, 6, 1), null));

        assertThat(foundInDublin).hasSize(1);
        assertThat(notFoundInUtc).isEmpty();
    }

    @Test
    void combinedSpecs_originAndDate_returnsOnlyMatch() {
        OffsetDateTime june1 = OffsetDateTime.parse("2030-06-01T10:00:00Z");
        OffsetDateTime june2 = OffsetDateTime.parse("2030-06-02T10:00:00Z");

        flightRepository.save(buildFlight(Airport.DUB, Airport.LHR, june1));  // match
        flightRepository.save(buildFlight(Airport.DUB, Airport.LHR, june2));  // wrong date
        flightRepository.save(buildFlight(Airport.LHR, Airport.DUB, june1));  // wrong origin

        List<Flight> result = flightRepository.findAll(
                Specification.where(notDeleted())
                        .and(hasOrigin(Airport.DUB))
                        .and(departingOnDate(LocalDate.of(2030, 6, 1), null)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrigin()).isEqualTo(Airport.DUB);
        assertThat(result.get(0).getDepartureTime().toInstant())
                .isEqualTo(june1.toInstant());
    }

    private Flight buildFlight(Airport origin, Airport destination, OffsetDateTime departureTime) {
        return Flight.builder()
                .flightNumber("T" + System.nanoTime())
                .origin(origin)
                .destination(destination)
                .departureTime(departureTime)
                .timezone("UTC")
                .seatRows(10)
                .seatColumns(6)
                .openForBookingUntil(departureTime.minusMinutes(45))
                .build();
    }
}
