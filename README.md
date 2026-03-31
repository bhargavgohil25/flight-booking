# Flight Ticket Booking API

A RESTful flight ticket booking system built with Spring Boot, featuring per-seat tracking, concurrent booking safety, strategy-based seat allocation, and integrated payment processing.

## How to Run

```bash
# Requires Java 17+
mvn clean install
mvn spring-boot:run
# Server starts on http://localhost:8080
```

## Pre-loaded Sample Flights

| Flight | Airline | Route | Total Seats | Available | Price/Seat |
|--------|---------|-------|-------------|-----------|------------|
| EK502 | Emirates | Mumbai → Dubai | 30 | ~25 | ₹25,000 |
| AI302 | Air India | Delhi → London | 42 | ~32 | ₹45,000 |
| SG101 | SpiceJet | Bangalore → Goa | 18 | 18 | ₹4,500 |
| 6E205 | IndiGo | Chennai → Kolkata | 24 | ~4 | ₹6,000 |
| UK833 | Vistara | Hyderabad → Mumbai | 12 | ~9 | ₹8,500 |
| QR501 | Qatar Airways | Delhi → Doha | 36 | ~28 | ₹32,000 |

*"~" indicates some seats are randomly pre-booked at startup.*

## Example Curl Requests

### Booking & Payment Flow

Booking is a two-step process:
1. **Create booking** — seats are reserved, status is `PENDING_PAYMENT`, a 10-minute payment deadline is set
2. **Process payment** — pay via UPI, CARD, or GIFT_CARD to confirm the booking

If payment is not completed within the deadline, the booking is automatically cancelled and seats are released.

```
PENDING_PAYMENT ──pay success──→ CONFIRMED
PENDING_PAYMENT ──pay failure──→ PAYMENT_FAILED (seats released)
PENDING_PAYMENT ──timeout───────→ CANCELLED     (seats released)
```

### Step 1: Create Booking (201 Created)
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "SG101",
    "passengerName": "John Doe",
    "passengerEmail": "john@example.com",
    "numberOfSeats": 2
  }'
```
Response includes `bookingId`, `status: "PENDING_PAYMENT"`, and `paymentDeadline`.

### Step 2: Process Payment (200 OK)

**Pay with UPI:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "UPI",
    "upiId": "john@okbank"
  }'
```

**Pay with Card:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CARD",
    "cardNumber": "4111111111111111"
  }'
```

**Pay with Gift Card:**
```bash
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "GIFT_CARD",
    "giftCardCode": "GIFT12345678"
  }'
```
Response includes `paymentStatus: "SUCCESS"`, `bookingStatus: "CONFIRMED"`, and `transactionReference`.

### Booking with Seat Preference (201 Created)
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "EK502",
    "passengerName": "Alice Smith",
    "passengerEmail": "alice@example.com",
    "numberOfSeats": 2,
    "seatPreference": "WINDOW"
  }'
```
Supported preferences: `WINDOW` (columns A, F), `AISLE` (columns C, D), `MIDDLE` (columns B, E). Preferred seats are assigned first; if not enough are available, remaining seats are filled from any available.

### Flight Not Found (404)
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "XX999",
    "passengerName": "John Doe",
    "passengerEmail": "john@example.com",
    "numberOfSeats": 1
  }'
```

### Insufficient Seats (409 Conflict)
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "6E205",
    "passengerName": "Big Group",
    "passengerEmail": "big@group.com",
    "numberOfSeats": 100
  }'
```

### Duplicate Booking (409 Conflict)
```bash
# Book once (succeeds)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "EK502",
    "passengerName": "Jane Doe",
    "passengerEmail": "jane@example.com",
    "numberOfSeats": 1
  }'

# Book again with same email + flight (409)
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "EK502",
    "passengerName": "Jane Doe",
    "passengerEmail": "jane@example.com",
    "numberOfSeats": 1
  }'
```

### Invalid Input (400 Bad Request)
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "EK502"
  }'
```

### Payment — Booking Not Found (404)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/00000000-0000-0000-0000-000000000000/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "UPI", "upiId": "user@upi"}'
```

### Payment — Already Confirmed (400)
```bash
# Pay for an already-confirmed booking
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "UPI", "upiId": "user@upi"}'
```

### Payment — Invalid Payment Details (400)
```bash
# Missing UPI ID
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "UPI"}'

# Invalid card number
curl -X POST http://localhost:8080/api/v1/bookings/{bookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "CARD", "cardNumber": "123"}'
```

### Payment — Expired Booking (410 Gone)
If the 10-minute payment deadline has passed, the booking is cancelled and seats are released:
```bash
curl -X POST http://localhost:8080/api/v1/bookings/{expiredBookingId}/payments \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "UPI", "upiId": "user@upi"}'
```

### Add a New Flight (201 Created)
```bash
curl -X POST http://localhost:8080/api/v1/flights \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "BA143",
    "airline": "British Airways",
    "origin": "Mumbai",
    "destination": "London",
    "departureTime": "2026-04-25T10:00:00",
    "arrivalTime": "2026-04-25T18:00:00",
    "totalSeats": 180,
    "pricePerSeat": 55000.00
  }'
```

## Design Decisions

### ReentrantLock per Flight for Concurrency
Each flight gets its own `ReentrantLock` stored in a `ConcurrentHashMap`. This prevents overbooking by ensuring the availability check and seat assignment are atomic per flight, while allowing concurrent bookings on different flights. More granular and testable than `synchronized` blocks.

### Repository Pattern with Interfaces
`FlightRepository` and `BookingRepository` are interfaces backed by `ConcurrentHashMap`-based in-memory implementations. Switching to JPA/database requires only adding a new implementation — no service code changes. Also enables clean unit testing with mocks.

### DTO Separation
Request DTOs carry Jakarta validation annotations (`@NotBlank`, `@Email`, `@Min`). Response DTOs decouple the API contract from internal entities. `ErrorResponse` provides consistent error structure across all failure scenarios.

### Strategy Pattern for Seat Validation and Allocation
Three allocation strategies, each serving a different use case:
- **SequentialSeatAllocationStrategy** (default) — assigns first N available seats in row order (1A, 1B, ...)
- **RandomSeatAllocationStrategy** — shuffles available seats and picks N randomly
- **PreferredSeatAllocationStrategy** — prioritizes seats matching a user's preference (WINDOW/AISLE/MIDDLE), falls back to any available when preferred seats are exhausted

The default strategy (Sequential vs Random) is **config-driven** via `application.properties`:
```properties
booking.seat-allocation-strategy=sequential  # or "random"
```
Seat preference (WINDOW/AISLE/MIDDLE) is **user-driven** — passed as an optional field in the booking request. When a preference is provided, the service uses `PreferredSeatAllocationStrategy`; otherwise it uses the configured default.

`SeatValidationStrategy` separates availability checking from allocation, making validation rules independently swappable.

### Payment Integration with State Machine
Bookings follow a clear state machine: `PENDING_PAYMENT → CONFIRMED | PAYMENT_FAILED | CANCELLED`. This two-step flow (reserve seats, then pay) prevents scenarios where seats are allocated but never paid for. Key design choices:

- **10-minute payment deadline** — seats are held temporarily; if payment isn't completed, a `@Scheduled` task (every 30s) cancels the booking and releases seats back to `AVAILABLE`
- **Per-booking ReentrantLock** in `PaymentService` — prevents double-payment race conditions (two concurrent pay requests for the same booking)
- **Seat release on failure/expiry** — `PAYMENT_FAILED` and `CANCELLED` states trigger immediate seat release, ensuring no phantom bookings hold seats indefinitely

### Strategy Pattern for Payment Processing
`PaymentProcessor` interface with three implementations (`UpiPaymentProcessor`, `CardPaymentProcessor`, `GiftCardPaymentProcessor`). Each processor handles its own validation (UPI ID format, card number length, gift card code) and payment simulation. Processors are auto-discovered via Spring's `List<PaymentProcessor>` injection and indexed by `PaymentMethod` in a map for O(1) lookup. Adding a new payment method (e.g., NetBanking, Wallet) requires only a new `@Component` implementing `PaymentProcessor`.

### Factory for Booking Creation
`BookingFactory` encapsulates UUID generation, price computation, timestamp setting, and status assignment. Keeps the service focused on orchestration rather than object construction.

### Individual Seat Tracking over Simple Counter
Each seat has a label (e.g., "3C") and a status (`AVAILABLE`/`BOOKED`) in a `LinkedHashMap`. This enables real seat assignments returned to passengers, prevents phantom availability from counter drift, and supports future features like seat preferences.

## What I'd Improve With More Time

- Database persistence with Spring Data JPA + H2/PostgreSQL
- Real payment gateway integration (Razorpay, Stripe) replacing simulated processors
- Payment retry with idempotency keys to handle transient failures
- Booking cancellation and refund endpoints (mark seats back to `AVAILABLE`, reverse payment)
- `GET /api/v1/bookings/{bookingId}` for retrieval
- `GET /api/v1/flights/{flightNumber}/seats` for seat map visualization
- `GET /api/v1/bookings/{bookingId}/payments` for payment status check
- API documentation with Swagger/OpenAPI (springdoc-openapi)
- Rate limiting with Bucket4j or Resilience4j
- Pagination for future list endpoints
- Caching layer (Spring Cache / Caffeine) for flight data
- Containerization with Dockerfile and docker-compose
- CI/CD pipeline with GitHub Actions
- Request/response logging with MDC correlation IDs
- Webhook/callback support for async payment notifications
