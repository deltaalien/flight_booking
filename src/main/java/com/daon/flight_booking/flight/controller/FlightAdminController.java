package com.daon.flight_booking.flight.controller;

import com.daon.flight_booking.flight.dto.CreateFlightRequest;
import com.daon.flight_booking.flight.dto.FlightResponse;
import com.daon.flight_booking.flight.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/flights")
@RequiredArgsConstructor
@Tag(name = "Flights (admin)")
public class FlightAdminController {

    private final FlightService flightService;

    @PostMapping
    @Operation(summary = "Create flight")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flight created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<FlightResponse> createFlight(@RequestBody @Valid CreateFlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete flight")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Flight deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    public void deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
    }
}
