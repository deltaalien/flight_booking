package com.daon.flight_booking.flight.dto;

import com.daon.flight_booking.flight.domain.Airport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateFlightRequest {

    @NotBlank
    private String flightNumber;

    @NotNull
    private Airport origin;

    @NotNull
    private Airport destination;

    @NotNull
    @Schema(description = "Local departure time in the flight's timezone, e.g. 2030-06-01T10:00:00")
    private LocalDateTime departureTime;

    @NotBlank
    @Schema(description = "IANA timezone ID, e.g. Europe/Dublin")
    private String timezone;

    @Min(1)
    private int seatRows;

    @Min(1)
    private int seatColumns;
}
