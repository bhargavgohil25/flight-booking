# Test Cases — Flight Ticket Booking API

**Total: 82 tests** | All passing

```bash
mvn test -s .mvn/maven-settings.xml
```

---

## 1. Unit Tests — BookingServiceImpl (8 tests)

**File:** `src/test/java/com/flightbooking/service/BookingServiceImplTest.java`

Uses Mockito to mock all dependencies (repositories, strategies, factory, mapper).

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `bookFlight_success_returnsBookingResponse` | Valid booking request with 2 seats | Returns BookingResponse with correct bookingId, allocatedSeats `[1A, 1B]`, totalPrice `50000.00`, status `PENDING_PAYMENT` |
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

## 4. Strategy Tests — Payment Processors (20 tests)

**File:** `src/test/java/com/flightbooking/strategy/PaymentProcessorTest.java`

Direct unit tests for each payment processor's validation and processing logic.

### UPI Processor (6 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `upi_getPaymentMethod` | Check processor type | Returns `PaymentMethod.UPI` |
| 2 | `upi_validate_validId_noException` | UPI ID `john@okbank` | No exception thrown |
| 3 | `upi_validate_nullId_throws` | UPI ID is `null` | Throws `IllegalArgumentException`: "UPI ID is required" |
| 4 | `upi_validate_blankId_throws` | UPI ID is whitespace `"  "` | Throws `IllegalArgumentException`: "UPI ID is required" |
| 5 | `upi_validate_noAtSymbol_throws` | UPI ID `invalid-upi` (no `@`) | Throws `IllegalArgumentException`: "Invalid UPI ID format" |
| 6 | `upi_processPayment_returnsUpiPrefix` | Process 5000 payment | Returns transaction ref starting with `UPI-`, length 12 |

### Card Processor (7 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 7 | `card_getPaymentMethod` | Check processor type | Returns `PaymentMethod.CARD` |
| 8 | `card_validate_validNumber_noException` | Card `4111111111111111` | No exception thrown |
| 9 | `card_validate_withSpaces_noException` | Card `4111 1111 1111 1111` | No exception (spaces stripped) |
| 10 | `card_validate_nullNumber_throws` | Card number is `null` | Throws `IllegalArgumentException`: "Card number is required" |
| 11 | `card_validate_tooShort_throws` | Card `12345` (5 digits) | Throws `IllegalArgumentException`: "Invalid card number" |
| 12 | `card_validate_tooLong_throws` | Card with 20 digits | Throws `IllegalArgumentException`: "Invalid card number" |
| 13 | `card_validate_nonDigits_throws` | Card `4111abcd11111111` | Throws `IllegalArgumentException`: "Invalid card number" |
| 14 | `card_processPayment_returnsCardPrefix` | Process 10000 payment | Returns transaction ref starting with `CARD-`, length 13 |

### Gift Card Processor (6 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 15 | `giftCard_getPaymentMethod` | Check processor type | Returns `PaymentMethod.GIFT_CARD` |
| 16 | `giftCard_validate_validCode_noException` | Code `GIFT12345678` | No exception thrown |
| 17 | `giftCard_validate_nullCode_throws` | Code is `null` | Throws `IllegalArgumentException`: "Gift card code is required" |
| 18 | `giftCard_validate_blankCode_throws` | Code is whitespace `"  "` | Throws `IllegalArgumentException`: "Gift card code is required" |
| 19 | `giftCard_validate_tooShort_throws` | Code `ABC` (3 chars, min 8) | Throws `IllegalArgumentException`: "Invalid gift card code" |
| 20 | `giftCard_processPayment_returnsGcPrefix` | Process 8000 payment | Returns transaction ref starting with `GC-`, length 11 |

---

## 5. Unit Tests — PaymentServiceImpl (14 tests)

**File:** `src/test/java/com/flightbooking/service/PaymentServiceImplTest.java`

Uses Mockito mocks for repositories; real payment processor implementations.

### Happy Path (3 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `processPayment_upi_success` | Pay pending booking via UPI | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED`, txn ref starts with `UPI-`, amount = `50000.00` |
| 2 | `processPayment_card_success` | Pay pending booking via Card | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED`, txn ref starts with `CARD-` |
| 3 | `processPayment_giftCard_success` | Pay pending booking via Gift Card | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED`, txn ref starts with `GC-` |

### State Validation (4 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 4 | `processPayment_bookingNotFound_throws` | Pay non-existent booking | Throws `BookingNotFoundException` |
| 5 | `processPayment_alreadyConfirmed_throws` | Pay already-confirmed booking | Throws `PaymentException`: "already paid" |
| 6 | `processPayment_cancelled_throws` | Pay cancelled booking | Throws `BookingExpiredException` |
| 7 | `processPayment_paymentFailed_throws` | Pay booking in `PAYMENT_FAILED` state | Throws `PaymentException`: "payment has failed" |

### Expiry & Seat Release (1 test)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 8 | `processPayment_expired_cancelsAndThrows` | Pay booking past deadline | Throws `BookingExpiredException` with "expired". Booking status set to `CANCELLED`. Seats `1A`, `1B` released back to `AVAILABLE` |

### Payment Validation (3 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 9 | `processPayment_upiWithoutId_throws` | UPI payment with `null` UPI ID | Throws `IllegalArgumentException`: "UPI ID is required" |
| 10 | `processPayment_cardWithInvalidNumber_throws` | Card payment with `123` | Throws `IllegalArgumentException`: "Invalid card number" |
| 11 | `processPayment_giftCardWithShortCode_throws` | Gift card with `ABC` | Throws `IllegalArgumentException`: "Invalid gift card code" |

### Response Field Assertions (3 tests)

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 12 | `processPayment_responseContainsCorrectBookingId` | Successful UPI payment | Response `bookingId` matches input, `paymentId` is non-null, `processedAt` is non-null |
| 13 | `processPayment_amountMatchesBookingTotalPrice` | Successful Card payment | Response `amount` equals booking's `totalPrice` (`50000.00`), `paymentMethod` is `CARD` |
| 14 | `processPayment_upiInvalidFormat_throws` | UPI ID `no-at-symbol` | Throws `IllegalArgumentException`: "Invalid UPI ID format" |

---

## 6. Unit Tests — PaymentExpiryScheduler (3 tests)

**File:** `src/test/java/com/flightbooking/config/PaymentExpirySchedulerTest.java`

Uses Mockito mocks. Tests the `@Scheduled` method directly.

| # | Test | Description | Expected |
|---|------|-------------|----------|
| 1 | `cancelExpiredBookings_cancelsAndReleasesSeats` | One expired booking with seats `1A`, `1B` | Booking status → `CANCELLED`. Both seats → `AVAILABLE`. `save()` called once |
| 2 | `cancelExpiredBookings_noExpired_doesNothing` | No expired bookings found | `save()` never called. `findByFlightNumber()` never called |
| 3 | `cancelExpiredBookings_multipleExpired_cancelsAll` | Two expired bookings on same flight | Both set to `CANCELLED`. Both seats released. `save()` called twice |

---

## 7. Integration Tests — Booking HTTP Layer (6 tests)

**File:** `src/test/java/com/flightbooking/controller/BookingControllerIntegrationTest.java`

Full `@SpringBootTest` with `MockMvc`. Tests hit the real controller, service, and in-memory repositories.

| # | Test | Description | HTTP Status | Assertions |
|---|------|-------------|-------------|------------|
| 1 | `bookFlight_validRequest_returns201` | POST valid booking for flight `SG101` | `201 Created` | Response has `bookingId`, `flightNumber=SG101`, 2 `allocatedSeats`, `status=PENDING_PAYMENT`, `paymentDeadline` exists |
| 2 | `bookFlight_unknownFlight_returns404` | POST booking for non-existent `XX999` | `404 Not Found` | `message` = "Flight not found: XX999" |
| 3 | `bookFlight_insufficientSeats_returns409` | POST booking for 100 seats on `UK833` (12 seats) | `409 Conflict` | `message` exists with insufficient seats info |
| 4 | `bookFlight_missingFields_returns400` | POST with only `flightNumber`, missing name/email/seats | `400 Bad Request` | `message` contains validation errors |
| 5 | `bookFlight_duplicate_returns409` | POST same email + flight twice | `409 Conflict` | `message` = "Duplicate booking: passenger already booked on this flight" |
| 6 | `bookFlight_withWindowPreference_returnsWindowSeats` | POST booking with `seatPreference: WINDOW` on `QR501` | `201 Created` | `allocatedSeats` array has 2 entries |

---

## 8. Integration Tests — Payment HTTP Layer (13 tests)

**File:** `src/test/java/com/flightbooking/controller/PaymentIntegrationTest.java`

Full `@SpringBootTest` with `MockMvc`. Each test creates a fresh booking, then tests the payment endpoint.

### Happy Path — Full Book → Pay Flow (3 tests)

| # | Test | Description | HTTP Status | Assertions |
|---|------|-------------|-------------|------------|
| 1 | `fullFlow_bookThenPayWithUpi_returns200Confirmed` | Book, then pay with UPI `user@okbank` | `200 OK` | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED`, `transactionReference` exists, `paymentMethod=UPI` |
| 2 | `fullFlow_bookThenPayWithCard_returns200Confirmed` | Book, then pay with card `4111111111111111` | `200 OK` | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED`, `transactionReference` exists |
| 3 | `fullFlow_bookThenPayWithGiftCard_returns200Confirmed` | Book, then pay with gift card `GIFT12345678` | `200 OK` | `paymentStatus=SUCCESS`, `bookingStatus=CONFIRMED` |

### Error Scenarios (5 tests)

| # | Test | Description | HTTP Status | Assertions |
|---|------|-------------|-------------|------------|
| 4 | `payment_nonExistentBooking_returns404` | Pay for random UUID | `404 Not Found` | — |
| 5 | `payment_duplicatePayment_returns400` | Pay, then pay again for same booking | `400 Bad Request` | `message` = "Booking is already paid and confirmed" |
| 6 | `payment_missingUpiId_returns400` | UPI payment with `null` UPI ID | `400 Bad Request` | `message` = "UPI ID is required for UPI payment" |
| 7 | `payment_invalidCardNumber_returns400` | Card payment with `123` | `400 Bad Request` | `message` = "Invalid card number" |
| 8 | `payment_missingPaymentMethod_returns400` | JSON body without `paymentMethod` | `400 Bad Request` | — |

### Additional Validation & Format (5 tests)

| # | Test | Description | HTTP Status | Assertions |
|---|------|-------------|-------------|------------|
| 9 | `payment_invalidUpiFormat_returns400` | UPI ID `nope-no-at` (no `@`) | `400 Bad Request` | `message` = "Invalid UPI ID format" |
| 10 | `payment_giftCardEmptyCode_returns400` | Gift card with empty string `""` | `400 Bad Request` | `message` = "Gift card code is required for gift card payment" |
| 11 | `payment_cardWithSpaces_succeeds` | Card `4111 1111 1111 1111` (spaces) | `200 OK` | `paymentStatus=SUCCESS`, `transactionReference` exists |
| 12 | `fullFlow_bookingStatusIsPendingBeforePayment` | Create booking, check status | `201 Created` | `status=PENDING_PAYMENT`, `paymentDeadline` exists, `paymentId` is null |
| 13 | `fullFlow_paymentResponseContainsAllFields` | Full pay flow, check response fields | `200 OK` | All 8 fields present: `paymentId`, `bookingId`, `amount`, `paymentMethod`, `paymentStatus`, `bookingStatus`, `transactionReference`, `processedAt` |

---

## 9. Edge Case Tests (7 tests)

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

## 10. Concurrency Stress Tests (2 tests)

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
│   ├── BookingServiceImplTest.java              #  8 unit tests (Mockito)
│   └── PaymentServiceImplTest.java              # 14 unit tests (Mockito + real processors)
├── strategy/
│   ├── SeatAllocationStrategyTest.java          #  4 tests (Sequential + Random)
│   ├── PreferredSeatAllocationStrategyTest.java #  5 tests (WINDOW/AISLE/MIDDLE)
│   └── PaymentProcessorTest.java                # 20 tests (UPI/Card/GiftCard validation + processing)
├── config/
│   └── PaymentExpirySchedulerTest.java          #  3 tests (expiry cancellation + seat release)
├── controller/
│   ├── BookingControllerIntegrationTest.java    #  6 integration tests (MockMvc)
│   └── PaymentIntegrationTest.java              # 13 integration tests (MockMvc, full book→pay flows)
├── EdgeCaseTest.java                            #  7 edge case tests (MockMvc)
└── ConcurrencyTest.java                         #  2 concurrency stress tests
```

## Running Tests

```bash
# All tests
mvn test -s .mvn/maven-settings.xml

# Specific test class
mvn test -s .mvn/maven-settings.xml -Dtest=PaymentServiceImplTest

# Specific test method
mvn test -s .mvn/maven-settings.xml -Dtest=PaymentProcessorTest#upi_validate_noAtSymbol_throws

# All payment-related tests
mvn test -s .mvn/maven-settings.xml -Dtest="PaymentServiceImplTest,PaymentIntegrationTest,PaymentProcessorTest,PaymentExpirySchedulerTest"
```
