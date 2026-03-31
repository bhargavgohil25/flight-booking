# Flight Ticket Booking API — Complete Project Spec

Read `ASSIGNMENT.md` in this folder for the original assignment. This is a take-home for an SDE-2 role at eBay.

## Execution Instructions

Implement this in iterative commits. After each phase, git add all files and commit with a descriptive message containing the prompt/intent for that phase. Initialize git repo if not already done.

- **Phase 1**: Full implementation (models, services, repositories, controller, exception handling, tests, README)
- **Phase 2**: Run the app, fix any compilation/runtime errors, run all tests, fix any failing tests
- **Phase 3**: Add concurrency stress tests and additional edge case tests
- **Phase 4**: Polish README with curl examples, design decisions, and improvement roadmap

Each phase must be a separate git commit with message: `"AI Iteration N: <what was done>"`.

---

## Business Requirements

Specs are intentionally vague in the assignment. These are reasoned assumptions filling in every ambiguity.

### Flight Model
- Fields: flightNumber (unique identifier, e.g., "EK502"), airline (String), origin (String, e.g., "Mumbai"), destination (String, e.g., "Dubai"), departureTime (LocalDateTime), arrivalTime (LocalDateTime), totalSeats (int), pricePerSeat (BigDecimal), seatMap (`Map<String, SeatStatus>` — keys are seat labels like "1A", "1B", "2A", values are enum SeatStatus: AVAILABLE, BOOKED)
- The seatMap tracks every individual seat. Seat labels follow airline convention: rows numbered 1-N, columns lettered A-F (6 seats per row for economy). Example: a 30-seat flight has rows 1-5 with seats A-F → "1A", "1B", "1C", "1D", "1E", "1F", "2A", ... "5F"
- `getAvailableSeats()` is a derived method that counts entries in seatMap with status AVAILABLE — no separate availableSeats field to keep in sync
- Flights are pre-populated at application startup via a DataInitializer component (use @PostConstruct or CommandLineRunner) — load 5-6 realistic sample flights with varied seat availability (some nearly full with random seats pre-marked BOOKED, some fully empty)
- Flights are immutable reference data after creation (no update/delete APIs for the flight itself, but seatMap is mutable for bookings)
- flightNumber is the lookup key — all operations reference flights by this field

### Seat Model
- SeatStatus enum: AVAILABLE, BOOKED
- Seat labels are generated during flight creation based on totalSeats: rows 1 to ceil(totalSeats/6), columns A-F. If totalSeats is not a multiple of 6, the last row has fewer seats.
- Example: totalSeats=14 → rows 1-2 have A-F (12 seats), row 3 has A-B (2 seats) → total 14

### Booking Model
- Fields: bookingId (UUID, auto-generated via UUID.randomUUID()), passengerName (String), passengerEmail (String), flightNumber (String, reference to Flight), numberOfSeats (int, supports multi-seat booking in a single request), allocatedSeats (List<String>, the actual seat labels assigned — e.g., ["3A", "3B"]), totalPrice (BigDecimal, computed = numberOfSeats × pricePerSeat), bookingTime (LocalDateTime, auto-set to LocalDateTime.now()), status (BookingStatus enum: CONFIRMED)
- One booking = one passenger booking one or more seats on one flight
- The allocatedSeats list is populated by the SeatAllocationStrategy and returned in the API response so the passenger knows their seat assignments
- Duplicate booking by same passengerEmail on same flightNumber should be rejected (409 Conflict)
- bookingId is returned in the response for the client to reference

### API Endpoints

#### 1. POST /api/v1/bookings — Book seats on a flight
- Request body: `{ "flightNumber": "EK502", "passengerName": "John Doe", "passengerEmail": "john@example.com", "numberOfSeats": 2 }`
- **201 Created** on success → response body: full booking object with bookingId, flightNumber, passengerName, passengerEmail, numberOfSeats, allocatedSeats (list of seat labels), totalPrice, bookingTime, status
- **404 Not Found** if flightNumber doesn't exist in the system → ErrorResponse with message "Flight not found: EK999"
- **400 Bad Request** for invalid/missing input (blank name, invalid email, null fields, seats ≤ 0) → ErrorResponse with field-level validation messages
- **409 Conflict** if not enough seats available → ErrorResponse with message "Insufficient seats: requested 5, available 3"
- **409 Conflict** if duplicate booking (same email + same flight) → ErrorResponse with message "Duplicate booking: passenger already booked on this flight"

#### 2. POST /api/v1/flights — (Optional admin endpoint) Add a new flight
- Request body: `{ "flightNumber": "AI302", "airline": "Air India", "origin": "Delhi", "destination": "London", "departureTime": "...", "arrivalTime": "...", "totalSeats": 200, "pricePerSeat": 45000.00 }`
- 201 Created → flight object in response
- 409 Conflict if flightNumber already exists
- Useful for testing flexibility, but pre-loaded data via DataInitializer is the primary mechanism

### Concurrency (CRITICAL — overbooking is explicitly forbidden by the assignment)
- Use ReentrantLock per flight stored in a `ConcurrentHashMap<String, ReentrantLock>` — one lock per flightNumber
- Lock scope must be wrapped in try-finally:
  ```java
  ReentrantLock lock = flightLocks.computeIfAbsent(flightNumber, k -> new ReentrantLock());
  lock.lock();
  try {
      // 1. Check seat availability (flight.getAvailableSeats() >= requestedSeats)
      // 2. Check duplicate booking
      // 3. Allocate seats via SeatAllocationStrategy (returns List<String> seatLabels)
      // 4. Mark allocated seats as BOOKED in flight's seatMap
      // 5. Create and save booking (with allocatedSeats list)
  } finally {
      lock.unlock();
  }
  ```
- This is preferred over synchronized blocks because it's more granular, explicit, and testable
- The lock must cover BOTH the availability check AND the decrement — a check-then-act without locking is a race condition

### Edge Cases to Handle
- Booking 0 or negative seats → 400 Bad Request
- numberOfSeats exceeds available seats (flight.getAvailableSeats()) → 409 Conflict with clear seat count message
- Flight number doesn't exist in repository → 404 Not Found
- Missing or blank passengerName → 400 Bad Request with validation message
- Missing or blank passengerEmail → 400 Bad Request with validation message
- Invalid email format → 400 Bad Request
- Duplicate booking (same passengerEmail + same flightNumber) → 409 Conflict
- numberOfSeats exceeds totalSeats (even on fully empty flight) → 409 Conflict
- Null request body → 400 Bad Request

---

## Architecture & Design Patterns (SOLID + Clean Code)

### Package Structure — follow this exactly
```
com.flightbooking
├── controller/
│   └── BookingController.java         # @RestController, thin — delegates to service
│   └── FlightController.java          # @RestController (optional, for adding flights)
├── service/
│   ├── BookingService.java            # Interface
│   └── BookingServiceImpl.java        # @Service, the single orchestrator
├── strategy/
│   ├── SeatValidationStrategy.java        # Interface — validates if booking is allowed
│   ├── DefaultSeatValidationStrategy.java # Checks available >= requested
│   ├── SeatAllocationStrategy.java        # Interface — decides WHICH seats to assign
│   ├── SequentialSeatAllocationStrategy.java  # Picks first N available seats in order (default)
│   └── RandomSeatAllocationStrategy.java      # Picks N random available seats
├── repository/
│   ├── FlightRepository.java          # Interface
│   ├── BookingRepository.java         # Interface
│   ├── InMemoryFlightRepository.java  # @Repository, ConcurrentHashMap-backed
│   └── InMemoryBookingRepository.java # @Repository, ConcurrentHashMap-backed
├── model/
│   ├── entity/
│   │   ├── Flight.java                # Domain object with seatMap
│   │   └── Booking.java               # Domain object with allocatedSeats
│   ├── enums/
│   │   ├── BookingStatus.java         # Enum: CONFIRMED
│   │   └── SeatStatus.java            # Enum: AVAILABLE, BOOKED
│   ├── request/
│   │   ├── BookingRequest.java        # DTO with Jakarta validation annotations
│   │   └── FlightRequest.java         # DTO (optional)
│   └── response/
│       ├── BookingResponse.java       # DTO returned to client (includes allocatedSeats)
│       ├── FlightResponse.java        # DTO returned to client
│       └── ErrorResponse.java         # Consistent error response DTO
├── exception/
│   ├── FlightNotFoundException.java       # extends RuntimeException
│   ├── InsufficientSeatsException.java    # extends RuntimeException
│   ├── DuplicateBookingException.java     # extends RuntimeException
│   └── GlobalExceptionHandler.java        # @RestControllerAdvice
├── config/
│   └── DataInitializer.java           # @Component, loads sample flights + generates seatMaps
├── mapper/
│   └── BookingMapper.java             # Entity ↔️ DTO conversion methods
└── factory/
    └── BookingFactory.java            # @Component, creates Booking objects with allocated seats
```

### Design Patterns — Apply these specifically

1. **Repository Pattern** — FlightRepository and BookingRepository as interfaces. InMemoryFlightRepository and InMemoryBookingRepository as implementations using ConcurrentHashMap as the backing store. Annotate implementations with @Repository. Interface methods: findByFlightNumber(String), save(Flight), existsByFlightNumber(String) for flights; save(Booking), existsByEmailAndFlightNumber(String, String) for bookings.

2. **Service Layer Pattern** — BookingService interface with a single method: `BookingResponse bookFlight(BookingRequest request)`. BookingServiceImpl is the SINGLE orchestrator — it coordinates the entire booking flow: validates input → checks flight exists via FlightRepository → acquires ReentrantLock → checks duplicate via BookingRepository → validates seat availability via SeatValidationStrategy → allocates specific seats via SeatAllocationStrategy → marks allocated seats as BOOKED in flight's seatMap → creates booking (with allocated seat labels) via BookingFactory → saves via BookingRepository → maps to response via BookingMapper → releases lock → returns. No separate Facade class. The Service IS the orchestrator. Annotate with @Service.

3. **DTO Pattern** — BookingRequest has Jakarta validation annotations: @NotBlank for passengerName, @NotBlank @Email for passengerEmail, @NotBlank for flightNumber, @NotNull @Min(1) for numberOfSeats. BookingResponse is a clean DTO with all booking fields. ErrorResponse has: timestamp (LocalDateTime), status (int), error (String), message (String), path (String). Never expose entity classes directly in API responses — always map through DTOs.

4. **Factory Pattern** — BookingFactory as a Spring @Component that encapsulates Booking object creation. Method: `Booking createBooking(BookingRequest request, Flight flight, List<String> allocatedSeats)` — generates UUID, sets allocatedSeats list, computes totalPrice (numberOfSeats × pricePerSeat), sets bookingTime to now, sets status to CONFIRMED. This isolates construction logic from the service.

5. **Singleton Pattern** — All Spring beans (@Service, @Repository, @Component) are singletons by default. The in-memory repositories holding ConcurrentHashMaps are naturally singletons managed by Spring's IoC container. This is the Spring-managed Singleton pattern — no manual getInstance() needed.

6. **Global Exception Handler** — @RestControllerAdvice class GlobalExceptionHandler with @ExceptionHandler methods for:
   - FlightNotFoundException → 404 with ErrorResponse
   - InsufficientSeatsException → 409 with ErrorResponse
   - DuplicateBookingException → 409 with ErrorResponse
   - MethodArgumentNotValidException → 400 with field-level validation errors in ErrorResponse
   - Generic Exception → 500 with ErrorResponse
   Each handler returns ResponseEntity<ErrorResponse> with consistent structure.

7. **Strategy Pattern (Validation)** — SeatValidationStrategy interface with method `void validate(Flight flight, int requestedSeats)`. DefaultSeatValidationStrategy implementation checks `flight.getAvailableSeats() >= requestedSeats`. Injected into BookingServiceImpl via constructor. This makes validation rules swappable (e.g., could add OverbookingAllowedStrategy, WaitlistStrategy in future).

8. **Strategy Pattern (Seat Allocation)** — SeatAllocationStrategy interface with method `List<String> allocateSeats(Flight flight, int numberOfSeats)`. Two implementations:
   - **SequentialSeatAllocationStrategy** (@Component, @Primary) — iterates the flight's seatMap in natural order (1A, 1B, ... 1F, 2A, ...) and picks the first N seats with status AVAILABLE. This is the default strategy. Predictable, deterministic, easy to test.
   - **RandomSeatAllocationStrategy** (@Component) — collects all AVAILABLE seats from seatMap, shuffles them using `Collections.shuffle()`, picks the first N. Useful for distributing passengers across the cabin.
   - Both strategies only SELECT seats — they return a `List<String>` of seat labels. The actual marking of seats as BOOKED happens in the service layer after allocation, inside the lock scope.
   - The active strategy is injected into BookingServiceImpl via constructor. @Primary on SequentialSeatAllocationStrategy makes it the default. To switch strategies, use @Qualifier or a configuration property.
   - This is the strongest pattern showcase in the project — two real, meaningfully different implementations, not just an interface with one impl.

### SOLID Principles — enforce these explicitly

- **Single Responsibility**: Controller handles HTTP request/response mapping only. Service handles business logic and orchestration only. Repository handles data storage/retrieval only. Factory handles object creation only. Mapper handles entity↔️DTO conversion only. Each class has exactly one reason to change.

- **Open/Closed**: Repository is an interface — switching from in-memory to JPA means adding JpaFlightRepository, not modifying InMemoryFlightRepository. Strategy pattern allows new validation rules and new seat allocation algorithms (e.g., PreferWindowSeatStrategy) without modifying existing code — just add a new implementation.

- **Liskov Substitution**: InMemoryFlightRepository can substitute FlightRepository anywhere without breaking behavior. SequentialSeatAllocationStrategy and RandomSeatAllocationStrategy both substitute SeatAllocationStrategy — same contract (returns N seat labels from available seats), different behavior (order vs randomness). DefaultSeatValidationStrategy can substitute SeatValidationStrategy without side effects.

- **Interface Segregation**: FlightRepository has only flight-related methods. BookingRepository has only booking-related methods. No god-interfaces. SeatValidationStrategy has a single focused method.

- **Dependency Inversion**: BookingServiceImpl depends on FlightRepository interface, BookingRepository interface, SeatValidationStrategy interface, SeatAllocationStrategy interface, BookingFactory, and BookingMapper — never on concrete implementations. All injected via constructor injection. Controller depends on BookingService interface, not BookingServiceImpl.

### Technical Standards

- **Constructor injection everywhere** — no @Autowired on fields. Use Lombok's @RequiredArgsConstructor with final fields, or explicit constructors.
- **Java Records for DTOs** where appropriate — BookingRequest, BookingResponse, ErrorResponse can be Records if no mutation is needed.
- **Proper logging** — SLF4J via Lombok @Slf4j on service and repository classes. Log at INFO level for successful bookings, WARN for business rule violations (insufficient seats, duplicates), ERROR for unexpected exceptions.
- **Lombok** — @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor on entity classes. @RequiredArgsConstructor on service/repository for constructor injection. @Slf4j for logging.
- **Input validation** — Use @Valid on controller method parameters. Jakarta validation annotations on request DTOs. Let Spring's validation framework handle 400 errors, caught by GlobalExceptionHandler.
- **Proper HTTP semantics** — POST returns 201 with Location header pointing to the created resource. Use ResponseEntity.created(URI) builder. All errors return appropriate 4xx/5xx status codes.
- **Thread safety** — All shared mutable state (ConcurrentHashMaps in repositories, flight seatMap mutations) must be accessed under proper synchronization via the ReentrantLock mechanism. The seatMap inside Flight is mutated (AVAILABLE → BOOKED) only inside the lock scope — never outside.

---

## Testing Requirements

### Unit Tests (JUnit 5 + Mockito)
Test BookingServiceImpl with mocked repositories:
1. **Happy path** — successful booking returns correct BookingResponse with allocated seat labels
2. **Flight not found** — throws FlightNotFoundException
3. **Insufficient seats** — throws InsufficientSeatsException
4. **Duplicate booking** — throws DuplicateBookingException
5. **Seats marked as BOOKED** — verify allocated seats in flight's seatMap are now BOOKED after booking
6. **Correct seats allocated** — SequentialSeatAllocationStrategy returns first N available seats in order
7. **Total price computed correctly** — numberOfSeats × pricePerSeat matches totalPrice in response
8. **Booking ID generated** — bookingId is non-null UUID
9. **Zero seats requested** — validation rejects with 400
10. **Allocated seats count matches** — allocatedSeats.size() == numberOfSeats in response

### Strategy-Specific Unit Tests
1. **SequentialSeatAllocationStrategy** — given a flight with some seats booked, verify it returns the first N available in natural order (e.g., if 1A and 1B are booked, requesting 2 seats returns ["1C", "1D"])
2. **RandomSeatAllocationStrategy** — given a flight, verify it returns N seats, all of which were AVAILABLE, and no duplicates in the returned list
3. **Both strategies** — requesting more seats than available throws InsufficientSeatsException (or returns insufficient count for service to handle)

### Integration Tests (@SpringBootTest + MockMvc)
Test the full HTTP layer:
1. POST /api/v1/bookings with valid data → 201 with booking response body
2. POST /api/v1/bookings with unknown flight → 404 with ErrorResponse
3. POST /api/v1/bookings with insufficient seats → 409 with ErrorResponse
4. POST /api/v1/bookings with missing fields → 400 with validation errors
5. POST /api/v1/bookings duplicate → 409 with ErrorResponse

### Concurrency Tests
1. **Race condition test** — Create a flight with exactly 5 available seats (e.g., row 1: A-E). Spawn 10 threads via ExecutorService, each trying to book 1 seat on the same flight simultaneously. Use CountDownLatch to synchronize thread start. Assert: exactly 5 bookings succeed (201), exactly 5 fail (409 insufficient seats), final getAvailableSeats() == 0, all 5 successful bookings have different seat labels (no duplicate seat assignments), and the union of all allocatedSeats covers exactly the 5 seats that were available.

---

## README.md Requirements

### Structure:
1. **Project Title + One-liner description**
2. **How to Run**:
   ```
   mvn clean install
   mvn spring-boot:run
   # Server starts on http://localhost:8080
   ```
3. **Pre-loaded Sample Flights** — table showing flightNumber, route, seats, price for each pre-loaded flight
4. **Example Curl Requests** — one for each scenario:
   - Successful booking (201)
   - Flight not found (404)
   - Insufficient seats (409)
   - Duplicate booking (409)
   - Invalid input / missing fields (400)
5. **Design Decisions**:
   - Why ReentrantLock per flight for concurrency (granular, explicit, testable vs synchronized)
   - Why Repository pattern with interfaces (swappable storage, testable with mocks)
   - Why DTO separation (decouples API contract from internal model, validation on request layer)
   - Why Strategy pattern for seat validation AND seat allocation (two strategies with real, different implementations — Sequential vs Random — not just pattern for pattern's sake)
   - Why Factory for booking creation (isolates construction, single responsibility)
   - Why individual seat tracking over simple counter (enables real seat assignments, prevents phantom availability, richer domain model)
6. **What I'd Improve With More Time**:
   - Database persistence with Spring Data JPA + H2/PostgreSQL
   - Seat class preferences (window/aisle/middle preference passed in request, SeatPreferenceStrategy)
   - Booking cancellation and refund endpoints (mark seats back to AVAILABLE)
   - GET /api/v1/bookings/{bookingId} for retrieval
   - GET /api/v1/flights/{flightNumber}/seats for seat map visualization
   - API documentation with Swagger/OpenAPI (springdoc-openapi)
   - Rate limiting with Bucket4j or Resilience4j
   - Pagination for future list endpoints
   - Caching layer (Spring Cache / Caffeine) for flight data
   - Containerization with Dockerfile and docker-compose
   - CI/CD pipeline with GitHub Actions
   - Request/response logging with MDC correlation IDs

---

## Step 2: Manual Improvement Guide (for after AI phase)

After all AI commits are done, open the code in your IDE and inspect these areas for manual fixes:

1. **Concurrency correctness** — Verify ReentrantLock is in try-finally. AI often forgets the finally block or places the lock.lock() inside the try (it should be before try).
2. **Validation gaps** — Check if @Email actually validates format. Add @Size constraints if missing. Verify numberOfSeats has @Max if reasonable.
3. **Response consistency** — All error responses must use the same ErrorResponse structure. Check that MethodArgumentNotValidException handler extracts field errors properly.
4. **HTTP status codes** — Verify 201 (not 200) for successful creation. Check if Location header is set. Verify 409 vs 400 usage is correct.
5. **Code cleanliness** — Remove any over-engineering the AI added (unnecessary interfaces, unused classes). If Strategy pattern feels forced, simplify but document why.
6. **Test quality** — Fix tests that verify implementation details instead of behavior. Ensure concurrency test actually uses CountDownLatch for synchronized start.
7. **Edge cases** — Test what happens with extremely large numberOfSeats, empty string vs null, whitespace-only names.
8. **Logging** — Ensure log messages are useful, not generic. Include flightNumber, bookingId, seat counts in log messages.

### Manual commit message format:
```
Manual improvements:
- Fixed: [specific things you fixed and why]
- Improved: [specific improvements and reasoning]
- Major issues with AI-generated code not fixed due to time: [list them with explanation]
```