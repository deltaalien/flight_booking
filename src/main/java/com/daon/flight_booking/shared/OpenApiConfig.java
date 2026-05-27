package com.daon.flight_booking.shared;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Flight Booking API",
                version = "1.0",
                description = "Seat reservation system for flights"
        ),
        security = @SecurityRequirement(name = "basicAuth")
)
public class OpenApiConfig {}
