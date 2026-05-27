package com.daon.flight_booking.booking.dto;

import com.daon.flight_booking.booking.domain.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BookingResponse {

    private UUID id;
    private Long flightId;
    private String flightNumber;
    private String passengerEmail;
    @Schema(description = "Seat in row@column format, e.g. 3@2")
    private String seat;
    private BookingStatus status;
    @Schema(description = "IANA timezone of the flight, e.g. Europe/Dublin")
    private String timezone;
    @Schema(description = "Hold expiry time in the flight's local timezone")
    private LocalDateTime holdUntil;
    private LocalDateTime createdAt;

}
