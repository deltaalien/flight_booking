package com.daon.flight_booking.flight.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Airport origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Airport destination;

    @Column(name = "departure_time", nullable = false)
    private OffsetDateTime departureTime;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "seat_rows", nullable = false)
    private int seatRows;

    @Column(name = "seat_columns", nullable = false)
    private int seatColumns;

    @Column(name = "open_for_booking_until", nullable = false)
    private OffsetDateTime openForBookingUntil;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
