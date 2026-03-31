# Flight Ticket Booking API

A RESTful flight ticket booking system built with Spring Boot, featuring per-seat tracking, concurrent booking safety, and strategy-based seat allocation.

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

### Successful Booking (201 Created)
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
Two real, meaningfully different allocation strategies:
- **SequentialSeatAllocationStrategy** (default) — assigns first N available seats in row order (1A, 1B, ...)
- **RandomSeatAllocationStrategy** — shuffles available seats and picks N randomly

Both implement `SeatAllocationStrategy`, injected via constructor. `@Primary` on Sequential makes it the default; switch via `@Qualifier` or config. `SeatValidationStrategy` separates availability checking from allocation, making validation rules independently swappable.

### Factory for Booking Creation
`BookingFactory` encapsulates UUID generation, price computation, timestamp setting, and status assignment. Keeps the service focused on orchestration rather than object construction.

### Individual Seat Tracking over Simple Counter
Each seat has a label (e.g., "3C") and a status (`AVAILABLE`/`BOOKED`) in a `LinkedHashMap`. This enables real seat assignments returned to passengers, prevents phantom availability from counter drift, and supports future features like seat preferences.

## What I'd Improve With More Time

- Database persistence with Spring Data JPA + H2/PostgreSQL
- Seat class preferences (window/aisle/middle preference passed in request, `SeatPreferenceStrategy`)
- Booking cancellation and refund endpoints (mark seats back to `AVAILABLE`)
- `GET /api/v1/bookings/{bookingId}` for retrieval
- `GET /api/v1/flights/{flightNumber}/seats` for seat map visualization
- API documentation with Swagger/OpenAPI (springdoc-openapi)
- Rate limiting with Bucket4j or Resilience4j
- Pagination for future list endpoints
- Caching layer (Spring Cache / Caffeine) for flight data
- Containerization with Dockerfile and docker-compose
- CI/CD pipeline with GitHub Actions
- Request/response logging with MDC correlation IDs
