# TicketBook Flight Booking API

A production-grade Spring Boot REST API for in-memory flight ticket booking with thread-safe concurrent operations, configurable booking policies, and comprehensive error handling.

## 1. Project Overview and Scope

TicketBook is a demonstration of backend engineering best practices in the context of a small but realistic flight booking system. It showcases:

- **Overbooking Prevention**: Thread-safe seat reservation logic guarantees no overselling, even under high concurrency
- **Configurable Policies**: Runtime booking limits (max seats per passenger, max cancellations per booking) without code changes
- **Clean Architecture**: Domain-driven design with clear separation between API, application, and domain layers
- **Production Readiness**: Comprehensive validation, error handling, structured logging, and full test coverage

**Key Constraints:**
- No database or external services—all state is in-memory and volatile across restarts
- No user authentication or authorization
- No flight search capability (clients provide flight numbers directly)
- Booking cancellations are allowed once per booking (configurable)

## 2. Tech Stack and Prerequisites

- **Java 17** or higher
- **Spring Boot 3.3.5** with Spring Web and Spring Validation
- **Maven 3.9+** for dependency and build management
- **JUnit 5 + Mockito** for comprehensive unit and integration tests

**External Dependencies:**
- Spring Boot Starter Web (REST controller support, embedded Tomcat)
- Spring Boot Starter Validation (JSR-303 constraint validation)
- Spring Boot Starter Test (JUnit 5, Mockito)

**No external runtime dependencies**: The API runs completely standalone with no database, message broker, or external service dependencies.

## 3. Local Setup and Run Steps

### Prerequisites Check
```bash
java --version      # Verify Java 17+
mvn --version       # Verify Maven 3.9+
```

### Clone and Setup
```bash
cd /path/to/workspace
git clone <repository-url> TicketBook
cd TicketBook
```

### Build the Project
```bash
# Compile code, run all tests, and package
mvn clean package
```

### Start the Service
```bash
# Option 1: Run via Spring Boot Maven plugin
mvn spring-boot:run

# Option 2: Run the packaged JAR
java -jar target/ticketbook-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
2026-04-17T10:30:45.123Z  INFO 12345 --- [main] com.ticketbook.TicketBookApplication : Started TicketBookApplication
2026-04-17T10:30:45.456Z  INFO 12345 --- [main] com.ticketbook.flight.monitoring.FlightAvailabilityLogger : Flight availability at startup:
  Flight TB100: 5 total, 0 booked, 5 available
  Flight TB200: 3 total, 0 booked, 3 available
  Flight TB300: 2 total, 0 booked, 2 available
```

Service is ready to accept requests on `http://localhost:1234`.

### Stop the Service
```bash
# Press Ctrl+C in the terminal where the service is running
```

## 4. API Quickstart with Sample Commands

All requests require `Content-Type: application/json` and use port `1234`.

### Book a Ticket
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Alice Johnson",
    "seats": 2
  }'
```

**Expected Response (201 Created):**
```json
{
  "bookingId": 1,
  "flightNumber": "TB100",
  "passengerName": "Alice Johnson",
  "seats": 2
}
```

### Check Flight Availability
```bash
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

**Response (200 OK):**
```json
{
  "flightNumber": "TB100",
  "totalCapacity": 5,
  "bookedSeats": 2,
  "availableSeats": 3
}
```

### Attempt Overbooking (Expected to Fail)
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Bob Smith",
    "seats": 4
  }'
```

**Response (409 Conflict):**
```json
{
  "timestamp": "2026-04-17T10:35:22.123456Z",
  "status": 409,
  "error": "Conflict",
  "message": "Not enough seats available on flight TB100 (4 requested, 3 available)"
}
```

### Cancel a Booking
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/1
```

**Response (204 No Content):** No body, headers only.

### Check Non-Existent Booking (Expected to Fail)
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/99999
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-04-17T10:36:05.789123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found: 99999"
}
```

See [API.md](./API.md) for comprehensive endpoint contract details with all request/response examples.

## 5. Error Handling Model and Status Codes

The API uses standard HTTP status codes and returns structured error responses:

| Status | Scenario | Example |
|--------|----------|---------|
| **201** | Booking created successfully | New booking ID returned with full details |
| **204** | Booking cancelled successfully | No content returned |
| **200** | Availability query successful | Current flight capacity info returned |
| **400** | Invalid request (validation failure) | Missing required field, negative seat count |
| **404** | Resource not found | Flight number or booking ID does not exist |
| **409** | Business logic violation | Overbooking attempt, policy limit exceeded, duplicate cancellation |

### Error Response Format
All error responses follow this structure:
```json
{
  "timestamp": "2026-04-17T10:35:22.123456Z",
  "status": 409,
  "error": "Conflict",
  "message": "Business-specific error description"
}
```

### Validation Errors
Request validation failures return `400 Bad Request` with field-level details:
```json
{
  "timestamp": "2026-04-17T10:35:22.123456Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "flightNumber": "flightNumber is required",
    "seats": "seats must be at least 1"
  }
}
```

### Custom Exception Mapping
- `FlightNotFoundException` → 404 Not Found
- `BookingNotFoundException` → 404 Not Found
- `OverbookingException` → 409 Conflict
- `BookingPolicyViolationException` → 409 Conflict
- `MethodArgumentNotValidException` → 400 Bad Request

See [API.md](./API.md) for detailed failure scenarios.

## 6. Configuration Guide

All configuration is centralized in `application.properties`. No code changes are required to adjust limits.

### Ports and Endpoints
```properties
# Server port (default: 1234)
server.port=1234

# Application name (for logging and context)
spring.application.name=ticketbook
```

### Booking Policy Limits
```properties
# Maximum seats one passenger can book per transaction (default: 5)
ticketbook.booking.max-seats-per-passenger=5

# Maximum cancellations allowed per booking (default: 1)
# Set to 0 for no cancellations; set to -1+ for unlimited
ticketbook.booking.max-cancellations-per-booking=1
```

### Flight Inventory Source
Flights are loaded from `src/main/resources/flights.json` at startup:

```json
[
  {
    "flightNumber": "TB100",
    "capacity": 5
  },
  {
    "flightNumber": "TB200",
    "capacity": 3
  },
  {
    "flightNumber": "TB300",
    "capacity": 2
  }
]
```

**To modify flights:**
1. Edit `src/main/resources/flights.json`
2. Rebuild and restart the service
3. Existing bookings are preserved during reload if the flight number remains unchanged

**Validation Rules Applied to `flights.json`:**
- Flight numbers must not be blank
- Flight numbers are normalized (case-insensitive, trimmed)
- No duplicate flight numbers (case-insensitive)
- Capacity must be at least 1

### Property Validation
Configuration properties are validated at startup using JSR-303 constraints:

```properties
# Invalid: Will fail at startup
ticketbook.booking.max-seats-per-passenger=0           # ❌ Must be >= 1

# Invalid: Will fail at startup
ticketbook.booking.max-cancellations-per-booking=-2    # ❌ Must be >= 0

# Valid: Will start successfully
ticketbook.booking.max-seats-per-passenger=5           # ✅ >= 1
ticketbook.booking.max-cancellations-per-booking=1     # ✅ >= 0
```

Invalid configurations abort startup with clear error messages.

## 7. Testing Guide

### Test Coverage

The project includes **29 automated tests** covering:

| Area | Test Classes | Focus |
|------|--------------|-------|
| **Configuration** | `ConfigurationPropertiesTest` | Property binding, constraint validation |
| **Booking Policy** | `BookingPolicyIntegrationTest` | Seat limits, cancellation limits |
| **Booking Service** | `BookingManagementServiceTest` | Booking workflow, flight lookups, overbooking prevention |
| **Flight Registry** | `FlightRegistryTest` | Hot-reload with seat preservation |
| **Flight Inventory** | `FlightInventoryLoaderTest` | JSON parsing, duplicate detection, capacity validation |
| **API Layer** | `BookingControllerTest` | All endpoints, HTTP status codes, request/response payloads |

### Run All Tests
```bash
# Run all tests with detailed output
mvn clean test

# Run specific test class
mvn test -Dtest=BookingControllerTest

# Run a single test method
mvn test -Dtest=BookingControllerTest#testCreateBookingSuccessfully
```

### Expected Test Results
```
[INFO] Running com.ticketbook.config.ConfigurationPropertiesTest
[INFO] Running com.ticketbook.booking.application.BookingPolicyIntegrationTest
[INFO] Running com.ticketbook.booking.application.BookingManagementServiceTest
[INFO] Running com.ticketbook.flight.registry.FlightRegistryTest
[INFO] Running com.ticketbook.flight.registry.FlightInventoryLoaderTest
[INFO] Running com.ticketbook.booking.api.BookingControllerTest
[INFO] 
[INFO] BUILD SUCCESS
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
```

### Key Test Scenarios

**Overbooking Prevention (Concurrent Test):**
```
✓ 10 threads attempt simultaneous bookings on TB300 (capacity: 2)
✓ Only 2 succeed, 8 receive 409 Conflict
✓ No double-booking or race conditions
```

**Policy Limit Enforcement:**
```
✓ Booking 6 seats when max is 5 → 409 Conflict
✓ Cancelling twice when max cancellations = 1 → 409 Conflict
✓ Valid bookings within limits → 201 Created
```

**JSON Inventory Loading:**
```
✓ Valid flights.json → Loaded with correct capacities
✓ Duplicate flight numbers → Fails at startup
✓ Blank flight numbers → Fails at startup
✓ Invalid capacity → Fails at startup
```

### Test Execution Flow
1. **Startup**: Configuration validated, flights loaded from JSON, in-memory registry initialized
2. **Tests Executed**: Thread pool isolated per test, concurrent scenarios validated
3. **Cleanup**: In-memory state cleared between tests (no persistence artifacts)

### Integration Test Example
```bash
# Start service in one terminal
mvn spring-boot:run

# Run tests in another terminal
mvn test

# Both should show consistent behavior:
# Service logs show flight availability updates
# Tests verify exact seat counts changed correctly
```

## 8. Assumptions, Constraints, and Known Limitations

### Design Assumptions
1. **Single Node Deployment**: No distributed consensus; all state is local to one process
2. **In-Memory Only**: State is lost on shutdown; no persistence across restarts
3. **Synchronous Workflow**: No async processing, message queues, or event sourcing
4. **Default Seat Count**: If `seats` field is omitted in request, defaults to 1
5. **Flight Number Format**: Alphanumeric, case-insensitive (TB100, tb100, Tb100 are identical)

### Constraints
- **No Authentication**: Every client can book, cancel, or query any flight
- **No Rate Limiting**: No protection against DoS or brute-force attacks
- **No Audit Trail Export**: Audit log exists in-memory only; not queryable via API
- **No Refund Logic**: Cancellations are permanent; no partial refunds or credits
- **No Concurrency Limits**: Able to handle many concurrent requests (tested with 10+), but no backpressure or queue management

### Known Limitations
| Limitation | Impact | Workaround |
|-----------|--------|-----------|
| No database persistence | All state lost on restart | Restart = clean slate for testing |
| In-memory audit log | Audit history lost on shutdown | Log to stdout for observability during session |
| No search endpoint | Must know flight number in advance | Client maintains flight directory |
| Single cancellation allowed | Cannot retry cancellation workflow | Idempotency keys not implemented |
| No availability notifications | Clients must poll for seat updates | Check before each booking attempt |

### Configuration Constraints
- `maxSeatsPerPassenger` must be ≥ 1 (enforced at startup)
- `maxCancellationsPerBooking` must be ≥ 0 (enforced at startup)
- Flight capacities must be ≥ 1 (enforced at startup from JSON)

## 9. "If I Had More Time" Roadmap

### High Priority (Production-Ready)
- [ ] **Persistence**: Replace in-memory maps with Spring Data + PostgreSQL/H2
- [ ] **Idempotency**: Add idempotency key support to prevent duplicate bookings on retries
- [ ] **Structured Logging**: Migrate to SLF4J + JSON formatting (e.g., Logback) for ELK/Datadog
- [ ] **Health/Actuator**: Add Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`)
- [ ] **API Versioning**: Version endpoints (`/api/v1/bookings`) for backward compatibility

### Medium Priority (Enhanced UX)
- [ ] **Search Endpoint**: `GET /api/flights` with filtering by route, date, price range
- [ ] **Seat Selection**: Replace simple count with allocated seat list (e.g., "1A", "1B", "2A")
- [ ] **Passenger Management**: Manage multiple passengers per booking; save PNR (Passenger Name Record)
- [ ] **Baggage Allowance**: Track and apply baggage limits per booking
- [ ] **Webhook Notifications**: Alert clients when flights are fully booked or seat availability changes

### Lower Priority (Nice-to-Have)
- [ ] **Admin Endpoints**: Reload flights.json without restarting; view booking statistics
- [ ] **Caching**: Cache flight objects with TTL for faster lookups
- [ ] **Rate Limiting**: Throttle requests per IP or API key using Spring Cloud
- [ ] **Distributed Locking**: Support multi-node deployments with Redis for synchronization
- [ ] **Payment Integration**: Stripe/PayPal integration for real payments

### Testing Expansion
- [ ] **Load Testing**: Simulate 1000+ concurrent bookings (Apache JMeter, Gatling)
- [ ] **Chaos Testing**: Inject failures (FlightRegistry fail-fast, network delays) and verify recovery
- [ ] **Contract Testing**: Consumer-driven tests with API clients (Pact framework)
- [ ] **Performance Benchmarks**: JMH (Java Microbenchmark Harness) for critical paths

### Infrastructure & DevOps
- [ ] **Container Image**: Dockerfile + Docker Compose for local dev and production
- [ ] **CI/CD Pipeline**: GitHub Actions / GitLab CI for automated test, build, deploy
- [ ] **Kubernetes Deployment**: Helm charts for multi-node orchestration with service mesh
- [ ] **Observability**: OpenTelemetry integration for tracing and metrics

---

## Quick Reference

| Task | Command |
|------|---------|
| Start service | `mvn spring-boot:run` |
| Run tests | `mvn clean test` |
| Build JAR | `mvn clean package` |
| Book flight | `curl -X POST http://localhost:1234/api/bookings -d '{"flightNumber":"TB100","passengerName":"Alice","seats":2}'` |
| Check availability | `curl http://localhost:1234/api/flights/TB100/availability` |
| Cancel booking | `curl -X DELETE http://localhost:1234/api/bookings/1` |

---

For detailed API contract specifications, see [API.md](./API.md).

For architecture decisions and design patterns, see [ARCHITECTURE.md](./ARCHITECTURE.md).
