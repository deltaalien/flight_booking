package com.daon.flight_booking.booking.repository;

import com.daon.flight_booking.booking.domain.Booking;
import com.daon.flight_booking.booking.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus WHERE b.status = :currentStatus AND b.holdUntil < :now")
    int cancelExpiredHolds(@Param("currentStatus") BookingStatus currentStatus,
                           @Param("newStatus") BookingStatus newStatus,
                           @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus WHERE b.flight.id = :flightId AND b.status IN :activeStatuses")
    int cancelActiveBookingsForFlight(@Param("flightId") Long flightId,
                                      @Param("activeStatuses") Collection<BookingStatus> activeStatuses,
                                      @Param("newStatus") BookingStatus newStatus);
}
