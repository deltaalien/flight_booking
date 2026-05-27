# Flight Booking

## Running

**With Docker (recommended)**

```bash
docker compose up --build
```

**Without Docker:** requires Java 21, Maven, and a running PostgreSQL instance configured via environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

```bash
./mvnw spring-boot:run
```

**Tests**

```bash
./mvnw test
```

Testcontainers manages its own database — Docker must be running.

## Seed accounts

| Username | Password | Role  |
|----------|----------|-------|
| alice    | admin123 | ADMIN |
| bob      | user123  | USER  |

## API documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

HTTP Client examples: [`api-examples.http`](api-examples.http)

## Key design decisions

**Partial unique index for seat uniqueness**\
The `bookings` table has a partial unique index on `(flight_id, seat) WHERE status IN ('HELD', 'CONFIRMED')`. 
Seat uniqueness is enforced at the database level. Canceled seats are automatically available for rebooking because the index ignores them.
This approach also simplifies handling of race conditions when there are multiple booking requests for the same seat, since the DB will just throw
a violation for the partial index. Another plus for this approach is that we don't need to hard-delete a booking.

**Pessimistic locking on confirm and cancel**\
`confirmBooking` and `cancelBooking` acquire a row-level lock (`SELECT FOR UPDATE`) before checking status. 
Concurrent attempts on the same booking are serialized — exactly one wins.

**Departure time stored as UTC instant + IANA timezone string**\
`departureTime` and `openForBookingUntil` are stored as `TIMESTAMP WITH TIME ZONE` (UTC-normalized). 
A separate `timezone` column (e.g. `Europe/Belgrade`) preserves the flight's local zone (for simplicity, airport timezones are not precalculated,
so this can lead to inconsistency in the flight table). 
The service accepts `LocalDateTime + timezone` at flight creation; then the service converts to `OffsetDateTime` via `ZoneId.of(timezone)` 
before persisting. Responses render times back in the flight's local zone using the same timezone string.
This is what makes the 45-minute booking window correct for multiple timezones: 
the cutoff is computed once at creation time in the departure airport's local time and stored as an absolute UTC timestamp, and on booking request
is compared against `OffsetDateTime.now()` captured at request time.

**Soft deletes for flights**\
Deleting a flight marks it `deleted = true` and bulk-cancels all active bookings in the same transaction. 
The flight record is preserved for historical integrity of booking data. 

**Single-statement booking expiry sweep**\
The hold expiry scheduler issues one `UPDATE bookings SET status = 'CANCELLED' WHERE status = 'HELD' AND hold_until < now()` per run. 
No entity loading, no individual transactions per booking.
One downside of this scheduled sweep is a small window (up to one cron interval) where an expired-but-unconfirmed booking still blocks the seat. 
`holdUntil` also prevents confirmation of booking after hold period expires. 

## Limitations and potential improvements

**Distributed scheduler**\
The hold-expiry `@Scheduled` job runs on every node in a multi-instance deployment, causing redundant (though idempotent) bulk UPDATEs. 
Adding `ShedLock` with a DB or Redis backend would elect one node per tick.

**Redis for hold management**\
Hold expiry currently relies on a periodic sweep, leaving a window (up to one cron interval) where an expired booking hasn't been released yet. 
An alternative is to store holds as Redis keys with a TTL equal to `holdMinutes`.

**In-memory cache**\
`GET /flights` uses Spring's default `ConcurrentHashMapCacheManager`, no TTL, no size cap, and it is invalidated globally on any flight mutation. 
This is broken in a multi-node deployment. Caffeine would be a good fit for single-node (TTL-based eviction), Redis for distributed.

**Airport timezone not derived from airport**\
The `timezone` field is a free-text IANA string supplied by the caller. 
Nothing prevents a DUB flight from being created with `timezone=America/New_York`, which would silently produce a wrong booking window. 
A production system would maintain a mapping from the `Airport` enum to its canonical IANA timezone and derive it automatically on flight creation.
A simple solution would be to add another table that would contain Airport + timezone mapping, and reference it in flights table.