package com.daon.flight_booking.booking.dto;

import com.daon.flight_booking.booking.domain.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingsMap {

    private final long flightId;
    private final SeatStatus[][] seats;

    @Schema(description = "O = Open, H = Temporarily reserved, B = Booked")
    public enum SeatStatus {
        O, H, B;

        public static SeatStatus fromBookingStatus(BookingStatus bookingStatus) {
            return switch (bookingStatus) {
                case BookingStatus.HELD -> H;
                case BookingStatus.CONFIRMED -> B;
                default -> O;
            };
        }
    }
}
