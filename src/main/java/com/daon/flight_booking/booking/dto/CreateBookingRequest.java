package com.daon.flight_booking.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {

    @Schema(hidden = true)
    private Long userId;

    @NotNull
    @Min(1)
    private Integer row;

    @NotNull
    @Min(1)
    private Integer column;

}
