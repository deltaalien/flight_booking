package com.daon.flight_booking.flight.service;

import com.daon.flight_booking.shared.BookingProperties;
import com.daon.flight_booking.booking.service.BookingService;
import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.dto.CreateFlightRequest;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.exception.FlightNotFoundException;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.shared.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    FlightRepository flightRepository;
    @Mock
    BookingService bookingService;
    @Mock
    BookingProperties bookingProperties;

    @InjectMocks
    FlightService flightService;

    @BeforeEach
    void setUp() {
        lenient().when(bookingProperties.getCutoffMinutes()).thenReturn(45);
    }

    @Test
    void createFlight_validRequest_returnsMappedResponse() {
        CreateFlightRequest request = buildRequest("Europe/Dublin");
        OffsetDateTime expectedDeparture = TimeUtil.toOffsetDateTime(
                request.getDepartureTime(), request.getTimezone());
        Flight savedFlight = Flight.builder()
                .id(1L)
                .flightNumber(request.getFlightNumber())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureTime(expectedDeparture)
                .timezone(request.getTimezone())
                .seatRows(request.getSeatRows())
                .seatColumns(request.getSeatColumns())
                .openForBookingUntil(expectedDeparture.minusMinutes(45))
                .build();
        when(flightRepository.save(any())).thenReturn(savedFlight);

        FlightResponse response = flightService.createFlight(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFlightNumber()).isEqualTo("EI100");
        assertThat(response.getOrigin()).isEqualTo(Airport.DUB);
        assertThat(response.getDestination()).isEqualTo(Airport.LHR);
        assertThat(response.getDepartureTime()).isEqualTo(request.getDepartureTime());
        assertThat(response.getTimezone()).isEqualTo("Europe/Dublin");
        assertThat(response.getSeatRows()).isEqualTo(20);
        assertThat(response.getSeatColumns()).isEqualTo(6);
        assertThat(response.getOpenForBookingUntil())
                .isEqualTo(TimeUtil.toLocalDateTime(savedFlight.getOpenForBookingUntil(), savedFlight.getTimezone()));
    }

    @Test
    void createFlight_invalidTimezone_throwsIllegalArgumentException() {
        CreateFlightRequest request = buildRequest("Bad/Zone");

        assertThatThrownBy(() -> flightService.createFlight(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bad/Zone");
    }

    @Test
    void createFlight_pastDeparture_throwsIllegalArgumentException() {
        CreateFlightRequest request = buildRequest("Europe/Dublin");
        request.setDepartureTime(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> flightService.createFlight(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createFlight_setsOpenForBookingUntilToCutoffBeforeDeparture() {
        CreateFlightRequest request = buildRequest("Europe/Dublin");
        OffsetDateTime departure = TimeUtil.toOffsetDateTime(request.getDepartureTime(), request.getTimezone());
        when(flightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        flightService.createFlight(request);

        ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).save(captor.capture());
        assertThat(captor.getValue().getOpenForBookingUntil())
                .isEqualTo(departure.minusMinutes(45));
    }

    @Test
    void deleteFlight_existingFlight_setsDeletedTrue() {
        Flight flight = Flight.builder()
                .id(1L)
                .flightNumber("EI100")
                .origin(Airport.DUB)
                .destination(Airport.LHR)
                .departureTime(OffsetDateTime.now().plusDays(30))
                .timezone("Europe/Dublin")
                .seatRows(20)
                .seatColumns(6)
                .openForBookingUntil(OffsetDateTime.now().plusDays(30).minusMinutes(45))
                .build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        flightService.deleteFlight(1L);

        assertThat(flight.isDeleted()).isTrue();
        verify(flightRepository).save(flight);
    }

    @Test
    void deleteFlight_cancelsActiveBookings() {
        Flight flight = Flight.builder()
                .id(1L)
                .flightNumber("EI100")
                .origin(Airport.DUB)
                .destination(Airport.LHR)
                .departureTime(OffsetDateTime.now().plusDays(30))
                .timezone("Europe/Dublin")
                .seatRows(20)
                .seatColumns(6)
                .openForBookingUntil(OffsetDateTime.now().plusDays(30).minusMinutes(45))
                .build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        flightService.deleteFlight(1L);

        verify(bookingService).cancelActiveBookingsForFlight(1L);
    }

    @Test
    void deleteFlight_nonExistingFlight_throwsFlightNotFoundException() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.deleteFlight(999L))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("999");
    }

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

    private CreateFlightRequest buildRequest(String timezone) {
        CreateFlightRequest request = new CreateFlightRequest();
        request.setFlightNumber("EI100");
        request.setOrigin(Airport.DUB);
        request.setDestination(Airport.LHR);
        request.setDepartureTime(LocalDateTime.now().plusDays(30));
        request.setTimezone(timezone);
        request.setSeatRows(20);
        request.setSeatColumns(6);
        return request;
    }
}
