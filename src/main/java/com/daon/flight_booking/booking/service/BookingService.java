package com.daon.flight_booking.booking.service;

import com.daon.flight_booking.booking.domain.Booking;
import com.daon.flight_booking.booking.domain.BookingStatus;
import com.daon.flight_booking.booking.dto.BookingResponse;
import com.daon.flight_booking.booking.dto.CreateBookingRequest;
import com.daon.flight_booking.booking.exception.BookingNotFoundException;
import com.daon.flight_booking.booking.exception.SeatUnavailableException;
import com.daon.flight_booking.booking.repository.BookingRepository;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.exception.FlightNotFoundException;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.shared.BookingProperties;
import com.daon.flight_booking.shared.TimeUtil;
import com.daon.flight_booking.user.domain.Role;
import com.daon.flight_booking.user.domain.User;
import com.daon.flight_booking.user.exception.UserNotFoundException;
import com.daon.flight_booking.user.repository.UserRepository;
import com.daon.flight_booking.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private static final String SEAT_FORMAT = "%d@%d";

    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingProperties bookingProperties;

    @Transactional
    public BookingResponse createBooking(Long flightId, CreateBookingRequest request, OffsetDateTime requestedAt, UserPrincipal principal) {

        long userId = principal.user().getId();
        if(isAdmin(principal) && request.getUserId() != null) {
            userId = request.getUserId();
        }

        Flight flight = flightRepository.findById(flightId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new FlightNotFoundException(flightId));

        if (requestedAt.isAfter(flight.getOpenForBookingUntil())) {
            throw new IllegalArgumentException("Booking window closed for this flight");
        }

        if (request.getRow() > flight.getSeatRows() || request.getColumn() > flight.getSeatColumns()) {
            throw new IllegalArgumentException("Seat out of range for this flight");
        }

        long finalUserId = userId;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(finalUserId));

        Booking booking = Booking.builder()
                .flight(flight)
                .user(user)
                .seat(String.format(SEAT_FORMAT, request.getRow(), request.getColumn()))
                .holdUntil(requestedAt.plusMinutes(bookingProperties.getHoldMinutes()))
                .createdAt(requestedAt)
                .build();

        try {
            return toResponse(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException e) {
            throw new SeatUnavailableException(request.getRow(), request.getColumn());
        }
    }

    @Transactional
    public int cancelActiveBookingsForFlight(Long flightId) {
        return bookingRepository.cancelActiveBookingsForFlight(flightId, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED), BookingStatus.CANCELLED);
    }

    @Transactional
    public int releaseExpiredHolds() {
        return bookingRepository.cancelExpiredHolds(BookingStatus.HELD, BookingStatus.CANCELLED, OffsetDateTime.now());
    }

    @Transactional
    public void cancelBooking(UUID bookingId, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));


        if (!isAdmin(principal) && isNotOwner(booking, principal)) {
            throw new AccessDeniedException("You do not own this booking");
        }

        if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
            throw new IllegalStateException("Booking cannot be cancelled from status " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, OffsetDateTime requestedAt, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!isAdmin(principal) && isNotOwner(booking, principal)) {
            throw new AccessDeniedException("You do not own this booking");
        }

        if (!booking.getStatus().canTransitionTo(BookingStatus.CONFIRMED)) {
            throw new IllegalStateException("Booking cannot be confirmed from status " + booking.getStatus());
        }

        if (requestedAt.isAfter(booking.getHoldUntil())) {
            throw new IllegalStateException("Hold has expired");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    private BookingResponse toResponse(Booking booking) {
        String tz = booking.getFlight().getTimezone();
        return BookingResponse.builder()
                .id(booking.getId())
                .flightId(booking.getFlight().getId())
                .flightNumber(booking.getFlight().getFlightNumber())
                .passengerEmail(booking.getUser().getEmail())
                .seat(booking.getSeat())
                .status(booking.getStatus())
                .timezone(tz)
                .holdUntil(TimeUtil.toLocalDateTime(booking.getHoldUntil(), tz))
                .createdAt(TimeUtil.toLocalDateTime(booking.getCreatedAt(), tz))
                .build();
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return Role.ADMIN == principal.user().getRole();
    }

    private static boolean isNotOwner(Booking booking, UserPrincipal userPrincipal) {
        long bookingOwner = booking.getUser().getId();
        long userId = userPrincipal.user().getId();
        return bookingOwner != userId;
    }
}
