package com.daon.flight_booking.booking.service;

import com.daon.flight_booking.booking.domain.Booking;
import com.daon.flight_booking.booking.domain.BookingStatus;
import com.daon.flight_booking.booking.dto.BookingResponse;
import com.daon.flight_booking.booking.dto.CreateBookingRequest;
import com.daon.flight_booking.booking.exception.BookingNotFoundException;
import com.daon.flight_booking.booking.exception.SeatUnavailableException;
import com.daon.flight_booking.shared.BookingProperties;
import com.daon.flight_booking.booking.repository.BookingRepository;
import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.exception.FlightNotFoundException;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.user.domain.Role;
import com.daon.flight_booking.user.domain.User;
import com.daon.flight_booking.user.exception.UserNotFoundException;
import com.daon.flight_booking.user.repository.UserRepository;
import com.daon.flight_booking.user.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock FlightRepository flightRepository;
    @Mock UserRepository userRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingProperties bookingProperties;

    @InjectMocks
    BookingService bookingService;

    @BeforeEach
    void setUp() {
        lenient().when(bookingProperties.getHoldMinutes()).thenReturn(15);
    }

    @Test
    void createBooking_validRequest_returnsMappedResponse() {
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        User user = User.builder().id(1L).email("alice@example.com").role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.createBooking(1L, buildRequest(1L, 5, 3), OffsetDateTime.now(), principalFor(user));

        assertThat(response.getSeat()).isEqualTo("5@3");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(response.getHoldUntil()).isAfter(LocalDateTime.now(ZoneId.of("Europe/Dublin")));
        assertThat(response.getPassengerEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void createBooking_flightNotFound_throwsFlightNotFoundException() {
        User user = User.builder().id(1L).role(Role.USER).build();
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(99L, buildRequest(1L, 1, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createBooking_deletedFlight_throwsFlightNotFoundException() {
        Flight deleted = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        deleted.setDeleted(true);
        User user = User.builder().id(1L).role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> bookingService.createBooking(1L, buildRequest(1L, 1, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(FlightNotFoundException.class);
    }

    @Test
    void createBooking_pastCutoff_throwsBookingWindowClosedException() {
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().minusHours(1));
        User user = User.builder().id(1L).role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(1L, buildRequest(1L, 1, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBooking_seatRowOutOfRange_throwsSeatOutOfRangeException() {
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        User user = User.builder().id(1L).role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(1L, buildRequest(1L, 99, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBooking_constraintViolation_throwsSeatUnavailableException() {
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        User user = User.builder().id(1L).email("alice@example.com").role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> bookingService.createBooking(1L, buildRequest(1L, 1, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("row 1")
                .hasMessageContaining("column 1");
    }

    @Test
    void createBooking_userNotFound_throwsUserNotFoundException() {
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        User user = User.builder().id(99L).role(Role.USER).build();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(1L, buildRequest(99L, 1, 1), OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void cancelBooking_existingBooking_setsStatusToCancelled() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).email("alice@example.com").name("Alice").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(user).seat("1@1")
                .status(BookingStatus.HELD)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        bookingService.cancelBooking(id, principalFor(user));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_notFound_throwsBookingNotFoundException() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).role(Role.USER).build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(id, principalFor(user)))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void cancelBooking_alreadyCancelled_throws409() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).email("alice@example.com").name("Alice").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(user).seat("1@1")
                .status(BookingStatus.CANCELLED)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(id, principalFor(user)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Booking cannot be cancelled from status CANCELLED");
    }

    @Test
    void cancelBooking_nonOwner_throwsAccessDeniedException() {
        UUID id = UUID.randomUUID();
        User owner = User.builder().id(1L).email("alice@example.com").role(Role.USER).build();
        User other = User.builder().id(2L).email("bob@example.com").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(owner).seat("1@1")
                .status(BookingStatus.HELD)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(id, principalFor(other)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmBooking_heldWithinWindow_setsConfirmedAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).email("alice@example.com").name("Alice").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(user).seat("1@1")
                .status(BookingStatus.HELD)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.confirmBooking(id, OffsetDateTime.now(), principalFor(user));

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmBooking_notFound_throwsBookingNotFoundException() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).role(Role.USER).build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(id, OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void confirmBooking_alreadyConfirmed_throws409() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).email("alice@example.com").name("Alice").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(user).seat("1@1")
                .status(BookingStatus.CONFIRMED)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(id, OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Booking cannot be confirmed from status CONFIRMED");
    }

    @Test
    void confirmBooking_holdExpired_throws409() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(1L).email("alice@example.com").name("Alice").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(user).seat("1@1")
                .status(BookingStatus.HELD)
                .holdUntil(OffsetDateTime.now().minusMinutes(5))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(id, OffsetDateTime.now(), principalFor(user)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Hold has expired");
    }

    @Test
    void confirmBooking_nonOwner_throwsAccessDeniedException() {
        UUID id = UUID.randomUUID();
        User owner = User.builder().id(1L).email("alice@example.com").role(Role.USER).build();
        User other = User.builder().id(2L).email("bob@example.com").role(Role.USER).build();
        Flight flight = buildFlight(20, 6, OffsetDateTime.now().plusDays(30));
        Booking booking = Booking.builder()
                .id(id).flight(flight).user(owner).seat("1@1")
                .status(BookingStatus.HELD)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .build();
        when(bookingRepository.findByIdForUpdate(id)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(id, OffsetDateTime.now(), principalFor(other)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Flight buildFlight(int seatRows, int seatColumns, OffsetDateTime openForBookingUntil) {
        return Flight.builder()
                .id(1L)
                .flightNumber("EI101")
                .origin(Airport.DUB)
                .destination(Airport.LHR)
                .departureTime(openForBookingUntil.plusMinutes(45))
                .timezone("Europe/Dublin")
                .seatRows(seatRows)
                .seatColumns(seatColumns)
                .openForBookingUntil(openForBookingUntil)
                .build();
    }

    private CreateBookingRequest buildRequest(Long userId, int row, int col) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setUserId(userId);
        request.setRow(row);
        request.setColumn(col);
        return request;
    }

    private UserPrincipal principalFor(User user) {
        return new UserPrincipal(user);
    }
}
