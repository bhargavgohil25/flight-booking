# Test Cases — Flight Ticket Booking API

**Total: 32 tests** | All passing

```bash
mvn test -s .mvn/maven-settings.xml
```

---

## 1. Unit Tests — BookingServiceImpl (8 tests)

**File:** `src/test/java/com/flightbooking/service/BookingServiceImplTest.java`

Uses Mockito to mock all dependencies (repositories, strategies, factory, mapper).

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `bookFlight_success_returnsBookingResponse` | Valid booking request with 2 seats | Returns BookingResponse with correct bookingId, allocatedSeats `[1A, 1B]`, totalPrice `50000.00`, status `CONFIRMED` |
| 2 | `bookFlight_flightNotFound_throwsException` | Request references non-existent flight `EK999` | Throws `FlightNotFoundException` with message "Flight not found: EK999" |
| 3 | `bookFlight_insufficientSeats_throwsException` | Request 50 seats on a 12-seat flight | Throws `InsufficientSeatsException` with message containing "Insufficient seats" |
| 4 | `bookFlight_duplicateBooking_throwsException` | Same email + same flight booked twice | Throws `DuplicateBookingException` with message "Duplicate booking: passenger already booked on this flight" |
| 5 | `bookFlight_seatsMarkedAsBooked` | Book 2 seats on a flight | Verifies `flight.getSeatMap()` shows `1A` and `1B` as `BOOKED` after booking |
| 6 | `bookFlight_totalPriceComputedCorrectly` | Book 2 seats at 25000/seat | `totalPrice` in response equals `50000.00` |
| 7 | `bookFlight_bookingIdGenerated` | Any successful booking | `bookingId` in response is non-null UUID |
| 8 | `bookFlight_allocatedSeatsCountMatchesRequest` | Book 3 seats | `allocatedSeats.size()` equals 3 and `numberOfSeats` equals 3 in response |

---

## 2. Strategy Tests — Sequential & Random Allocation (4 tests)

**File:** `src/test/java/com/flightbooking/strategy/SeatAllocationStrategyTest.java`

Tests run against a 12-seat flight (rows 1-2, columns A-F).

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `sequential_returnsFirstNAvailableInOrder` | Seats `1A`, `1B` pre-booked; request 2 | Returns `[1C, 1D]` (first available in natural order) |
| 2 | `sequential_returnsCorrectCountOfSeats` | No seats booked; request 4 | Returns `[1A, 1B, 1C, 1D]` |
| 3 | `random_returnsNAvailableSeatsNoDuplicates` | No seats booked; request 5 | Returns 5 unique seats, all with status `AVAILABLE` |
| 4 | `random_doesNotReturnBookedSeats` | Seats `1A`, `1B` pre-booked; request 3 | Returns 3 seats, none of which are `1A` or `1B` |

---

## 3. Strategy Tests — Preferred Seat Allocation (5 tests)

**File:** `src/test/java/com/flightbooking/strategy/PreferredSeatAllocationStrategyTest.java`

Tests run against a 12-seat flight. Column mapping: A,F = WINDOW; B,E = MIDDLE; C,D = AISLE.

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `windowPreference_returnsWindowSeatsFirst` | Request 3 with `WINDOW` preference | Returns `[1A, 1F, 2A]` (window columns first) |
| 2 | `aislePreference_returnsAisleSeatsFirst` | Request 3 with `AISLE` preference | Returns `[1C, 1D, 2C]` (aisle columns first) |
| 3 | `middlePreference_returnsMiddleSeatsFirst` | Request 3 with `MIDDLE` preference | Returns `[1B, 1E, 2B]` (middle columns first) |
| 4 | `windowPreference_fallsBackToOtherSeatsWhenNotEnough` | All 4 window seats booked; request 2 with `WINDOW` | Falls back to non-window seats `[1B, 1C]` |
| 5 | `preference_partialMatch_fillsPreferredThenFallback` | 3 of 4 window seats booked; request 3 with `WINDOW` | First seat is `1F` (remaining window), then 2 fallback seats |

---

## 4. Integration Tests — HTTP Layer (6 tests)

**File:** `src/test/java/com/flightbooking/controller/BookingControllerIntegrationTest.java`

Full `@SpringBootTest` with `MockMvc`. Tests hit the real controller, service, and in-memory repositories.

| # | Test | Description | HTTP Status | Assertions |
|---|------|-------------|-------------|------------|
| 1 | `bookFlight_validRequest_returns201` | POST valid booking for flight `SG101` | `201 Created` | Response has `bookingId`, `flightNumber=SG101`, 2 `allocatedSeats`, `status=CONFIRMED` |
| 2 | `bookFlight_unknownFlight_returns404` | POST booking for non-existent `XX999` | `404 Not Found` | `message` = "Flight not found: XX999" |
| 3 | `bookFlight_insufficientSeats_returns409` | POST booking for 100 seats on `UK833` (12 seats) | `409 Conflict` | `message` exists with insufficient seats info |
| 4 | `bookFlight_missingFields_returns400` | POST with only `flightNumber`, missing name/email/seats | `400 Bad Request` | `message` contains validation errors |
| 5 | `bookFlight_duplicate_returns409` | POST same email + flight twice | `409 Conflict` | `message` = "Duplicate booking: passenger already booked on this flight" |
| 6 | `bookFlight_withWindowPreference_returnsWindowSeats` | POST booking with `seatPreference: WINDOW` on `QR501` | `201 Created` | `allocatedSeats` array has 2 entries |

---

## 5. Edge Case Tests (7 tests)

**File:** `src/test/java/com/flightbooking/EdgeCaseTest.java`

Full `@SpringBootTest` with `MockMvc`. Validates input boundary conditions.

| # | Test | Description | HTTP Status |
|---|------|-------------|-------------|
| 1 | `zeroSeats_returns400` | `numberOfSeats: 0` | `400 Bad Request` |
| 2 | `negativeSeats_returns400` | `numberOfSeats: -1` | `400 Bad Request` |
| 3 | `blankName_returns400` | `passengerName: ""` | `400 Bad Request` |
| 4 | `invalidEmail_returns400` | `passengerEmail: "not-an-email"` | `400 Bad Request` |
| 5 | `nullRequestBody_returns400` | Empty JSON body `{}` | `400 Bad Request` |
| 6 | `seatsExceedTotalSeats_returns409` | Request 100 seats on 12-seat flight | `409 Conflict` |
| 7 | `whitespaceOnlyName_returns400` | `passengerName: "   "` | `400 Bad Request` |

---

## 6. Concurrency Stress Tests (2 tests)

**File:** `src/test/java/com/flightbooking/ConcurrencyTest.java`

Full `@SpringBootTest`. Uses `ExecutorService` and `CountDownLatch` to simulate concurrent booking attempts.

| # | Test | Description | Assertions |
|---|------|-------------|------------|
| 1 | `concurrentBookings_noOverbooking` | 10 threads each book 1 seat on a 5-seat flight (`RACE01`), all starting simultaneously via `CountDownLatch` | Exactly 5 succeed, 5 fail. Available seats = 0. No duplicate seat assignments across successful bookings. All 5 allocated seats are marked `BOOKED` in the flight's seatMap. |
| 2 | `concurrentMultiSeatBookings_noOverbooking` | 5 threads each book 2 seats on a 6-seat flight (`RACE02`), starting simultaneously | Exactly 3 succeed (6 / 2 = 3), 2 fail. Available seats = 0. |

---

## Test Structure Summary

```
src/test/java/com/flightbooking/
├── service/
│   └── BookingServiceImplTest.java          # 8 unit tests (Mockito)
├── strategy/
│   ├── SeatAllocationStrategyTest.java      # 4 tests (Sequential + Random)
│   └── PreferredSeatAllocationStrategyTest.java  # 5 tests (WINDOW/AISLE/MIDDLE)
├── controller/
│   └── BookingControllerIntegrationTest.java  # 6 integration tests (MockMvc)
├── EdgeCaseTest.java                         # 7 edge case tests (MockMvc)
└── ConcurrencyTest.java                      # 2 concurrency stress tests
```

## Running Tests

```bash
# All tests
mvn test -s .mvn/maven-settings.xml

# Specific test class
mvn test -s .mvn/maven-settings.xml -Dtest=ConcurrencyTest

# Specific test method
mvn test -s .mvn/maven-settings.xml -Dtest=BookingServiceImplTest#bookFlight_success_returnsBookingResponse
```
