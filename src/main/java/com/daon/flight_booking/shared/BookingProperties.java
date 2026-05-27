package com.daon.flight_booking.shared;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "booking")
public class BookingProperties {
    private int holdMinutes = 15;
    private int cutoffMinutes = 45;
    private String expiryCron = "0 * * * * *";
}
