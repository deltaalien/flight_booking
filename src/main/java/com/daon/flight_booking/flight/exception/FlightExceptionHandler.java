package com.daon.flight_booking.flight.exception;

import com.daon.flight_booking.flight.controller.FlightAdminController;
import com.daon.flight_booking.flight.controller.FlightController;
import com.daon.flight_booking.shared.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {FlightController.class, FlightAdminController.class})
public class FlightExceptionHandler extends BaseExceptionHandler {
}
