package com.daon.flight_booking.shared;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtil {

    private TimeUtil() {}

    public static OffsetDateTime toOffsetDateTime(LocalDateTime localTime, String timezone) {
        try {
            return localTime.atZone(ZoneId.of(timezone)).toOffsetDateTime();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone: " + timezone);
        }
    }

    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetTime, String timezone) {
        return offsetTime.atZoneSameInstant(ZoneId.of(timezone)).toLocalDateTime();
    }

    public static void validateTimezone(String timezone) {
        try {
            var ignore = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone: " + timezone);
        }
    }

}
