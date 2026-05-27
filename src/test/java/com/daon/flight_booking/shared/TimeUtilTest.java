package com.daon.flight_booking.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeUtilTest {

    @Test
    void toOffsetDateTime_dublinSummer_appliesCorrectOffset() {
        LocalDateTime local = LocalDateTime.of(2030, 6, 15, 14, 30);
        OffsetDateTime result = TimeUtil.toOffsetDateTime(local, "Europe/Dublin");
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(result.toLocalDateTime()).isEqualTo(local);
    }

    @Test
    void toOffsetDateTime_dstGap_advancesToPostGapTime() {
        LocalDateTime gapTime = LocalDateTime.of(2030, 3, 31, 1, 30);
        OffsetDateTime result = TimeUtil.toOffsetDateTime(gapTime, "Europe/Dublin");
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.ofHours(1));
        assertThat(result.getHour()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void toOffsetDateTime_invalidTimezone_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TimeUtil.toOffsetDateTime(LocalDateTime.now().plusDays(1), "Bad/Zone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bad/Zone");
    }

    @Test
    void toLocalDateTime_utcOffsetInDublinSummer_shiftsByOneHour() {
        OffsetDateTime utcTime = OffsetDateTime.of(2030, 6, 15, 13, 30, 0, 0, ZoneOffset.UTC);
        LocalDateTime result = TimeUtil.toLocalDateTime(utcTime, "Europe/Dublin");
        assertThat(result).isEqualTo(LocalDateTime.of(2030, 6, 15, 14, 30, 0));
    }

    @Test
    void toLocalDateTime_roundTrip_equalsOriginal() {
        LocalDateTime original = LocalDateTime.of(2030, 6, 15, 14, 30);
        OffsetDateTime converted = TimeUtil.toOffsetDateTime(original, "Europe/Dublin");
        assertThat(TimeUtil.toLocalDateTime(converted, "Europe/Dublin")).isEqualTo(original);
    }

    @Test
    void validateTimezone_invalidZone_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TimeUtil.validateTimezone("Not/AZone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not/AZone");
    }
}
