package com.daon.flight_booking.booking.service;

import com.daon.flight_booking.booking.dto.BookingResponse;
import com.daon.flight_booking.booking.dto.CreateBookingRequest;
import com.daon.flight_booking.booking.exception.SeatUnavailableException;
import com.daon.flight_booking.booking.repository.BookingRepository;
import com.daon.flight_booking.flight.domain.Airport;
import com.daon.flight_booking.flight.domain.Flight;
import com.daon.flight_booking.flight.repository.FlightRepository;
import com.daon.flight_booking.user.domain.Role;
import com.daon.flight_booking.user.domain.User;
import com.daon.flight_booking.user.repository.UserRepository;
import com.daon.flight_booking.user.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class BookingConcurrencyTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.contexts", () -> "test");
    }

    @Autowired FlightRepository flightRepository;
    @Autowired UserRepository userRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingService bookingService;

    Flight flight;
    User user;

    @BeforeEach
    void setUp() {
        flight = flightRepository.save(buildFlight());
        user = userRepository.save(buildUser());
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        flightRepository.delete(flight);
        userRepository.delete(user);
    }

    @Test
    void concurrentBookingSameSeat_exactlyOneSucceeds() throws InterruptedException {
        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    bookingService.createBooking(flight.getId(), request(1, 1), OffsetDateTime.now(), principal());
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }

    @Test
    void concurrentBookingDifferentSeats_allSucceed() throws InterruptedException {
        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int row = i + 1;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    bookingService.createBooking(flight.getId(), request(row, 1), OffsetDateTime.now(), principal());
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(threads);
        assertThat(conflicts.get()).isZero();
    }

    @Test
    void cancelledSeatCanBeRebooked_concurrently() throws InterruptedException {
        BookingResponse held = bookingService.createBooking(flight.getId(), request(1, 1), OffsetDateTime.now(), principal());
        bookingService.cancelBooking(held.getId(), principal());

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    bookingService.createBooking(flight.getId(), request(1, 1), OffsetDateTime.now(), principal());
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }

    @Test
    void concurrentConfirmSameBooking_exactlyOneSucceeds() throws InterruptedException {
        BookingResponse held = bookingService.createBooking(flight.getId(), request(2, 1), OffsetDateTime.now(), principal());
        UUID bookingId = held.getId();

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    bookingService.confirmBooking(bookingId, OffsetDateTime.now(), principal());
                    successes.incrementAndGet();
                } catch (IllegalStateException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }

    @Test
    void concurrentCancelSameBooking_exactlyOneSucceeds() throws InterruptedException {
        BookingResponse held = bookingService.createBooking(flight.getId(), request(3, 1), OffsetDateTime.now(), principal());
        UUID bookingId = held.getId();

        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    bookingService.cancelBooking(bookingId, principal());
                    successes.incrementAndGet();
                } catch (IllegalStateException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threads - 1);
    }

    private UserPrincipal principal() {
        return new UserPrincipal(user);
    }

    private CreateBookingRequest request(int row, int col) {
        CreateBookingRequest r = new CreateBookingRequest();
        r.setUserId(user.getId());
        r.setRow(row);
        r.setColumn(col);
        return r;
    }

    private Flight buildFlight() {
        OffsetDateTime dep = OffsetDateTime.now().plusDays(30);
        return Flight.builder()
                .flightNumber("CC" + System.nanoTime())
                .origin(Airport.DUB)
                .destination(Airport.LHR)
                .departureTime(dep)
                .timezone("Europe/Dublin")
                .seatRows(20)
                .seatColumns(6)
                .openForBookingUntil(dep.minusMinutes(45))
                .build();
    }

    private User buildUser() {
        long ts = System.nanoTime();
        return User.builder()
                .email("concurrent+" + ts + "@test.com")
                .name("Test User")
                .username("user" + ts)
                .password("$2a$10$irrelevant.hash.for.test.purposes.only.padding.here.XYZ")
                .role(Role.USER)
                .build();
    }
}
