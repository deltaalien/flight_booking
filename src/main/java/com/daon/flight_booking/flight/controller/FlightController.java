package com.daon.flight_booking.flight.controller;

import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
@Tag(name = "Flights (public)")
public class FlightController {

    private final FlightService flightService;

    @GetMapping
    @Operation(summary = "List flights", security = @SecurityRequirement(name = ""))
    @ApiResponse(responseCode = "200", description = "List of matching flights")
    public ResponseEntity<List<FlightResponse>> getFlights(
            @RequestParam(required = false) Airport origin,
            @RequestParam(required = false) Airport destination,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(flightService.getFlights(origin, destination, date, timezone));
    }
}
