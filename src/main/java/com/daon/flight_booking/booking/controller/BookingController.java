package com.daon.flight_booking.booking.controller;

import com.daon.flight_booking.booking.dto.BookingResponse;
import com.daon.flight_booking.booking.dto.BookingsMap;
import com.daon.flight_booking.booking.dto.CreateBookingRequest;
import com.daon.flight_booking.booking.service.BookingService;
import com.daon.flight_booking.user.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/flights/{id}/bookings")
    @Operation(summary = "Create booking")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created (HELD)"),
            @ApiResponse(responseCode = "400", description = "Invalid seat or booking window closed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
            @ApiResponse(responseCode = "409", description = "Seat already taken")
    })
    public ResponseEntity<BookingResponse> createBooking(
            @PathVariable Long id,
            @RequestBody @Valid CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(id, request, OffsetDateTime.now(), principal));
    }

    @GetMapping("/flights/{id}/bookings")
    @Operation(summary = "Get seat status for flight")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrix with statuses of seat"),
            @ApiResponse(responseCode = "404", description = "Flight not found"),
    })
    public ResponseEntity<BookingsMap> getBookingsForFlight(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingsMap(id));
    }

    @PostMapping("/bookings/{id}/confirm")
    @Operation(summary = "Confirm booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking confirmed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking cannot be confirmed (wrong status or hold expired)")
    })
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, OffsetDateTime.now(), principal));
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Cancel booking")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking cannot be cancelled (wrong status)")
    })
    public void cancelBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        bookingService.cancelBooking(id, principal);
    }
}
