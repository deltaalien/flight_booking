package com.daon.flight_booking.flight.service;

import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    FlightRepository flightRepository;

    @InjectMocks
    FlightService flightService;

    @Test
    @SuppressWarnings("unchecked")
    void getFlights_returnsMappedList() {
        Flight flight = Flight.builder()
                .id(10L)
                .flightNumber("EI101")
                .origin(Airport.DUB)
                .destination(Airport.LHR)
                .departureTime(OffsetDateTime.now().plusDays(10))
                .timezone("Europe/Dublin")
                .seatRows(20)
                .seatColumns(6)
                .openForBookingUntil(OffsetDateTime.now().plusDays(10).minusMinutes(45))
                .build();
        when(flightRepository.findAll(any(Specification.class))).thenReturn(List.of(flight));

        List<FlightResponse> result = flightService.getFlights(null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getFlightNumber()).isEqualTo("EI101");
    }

    @Test
    void getFlights_invalidTimezone_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> flightService.getFlights(null, null, null, "Bad/Zone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bad/Zone");
    }
}
