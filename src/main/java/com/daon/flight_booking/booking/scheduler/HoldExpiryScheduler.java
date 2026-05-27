package com.daon.flight_booking.booking.scheduler;

import com.daon.flight_booking.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(cron = "${booking.expiry-cron:0 * * * * *}")
    public void releaseExpiredHolds() {
        int released = bookingService.releaseExpiredHolds();
        if (released > 0) {
            log.info("Released {} expired booking holds", released);
        }
    }
}
