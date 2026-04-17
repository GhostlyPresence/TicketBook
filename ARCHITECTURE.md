# TicketBook Architecture

This document explains the design decisions, component structure, request flow, and concurrency strategy behind the TicketBook flight booking API.

---

## 1. Folder/Package Structure and Responsibilities

The project follows **Domain-Driven Design (DDD)** principles with bounded contexts for booking and flight management.

### Package Hierarchy

```
com.ticketbook
├── TicketBookApplication          [Spring Boot entry point, @ConfigurationPropertiesScan]
│
├── booking/                        [BOUNDED CONTEXT: Flight Booking]
│   ├── api/
│   │   ├── BookingController       [HTTP request handler, REST endpoints]
│   │   └── ApiExceptionHandler     [@RestControllerAdvice for global error handling]
│   │
│   ├── application/
│   │   └── BookingManagementService [Facade orchestrating booking workflow]
│   │
│   ├── domain/
│   │   └── BookingService          [Booking persistence & state management]
│   │
│   └── audit/
│       └── BookingAuditLog         [In-memory audit trail for compliance]
│
├── flight/                        [BOUNDED CONTEXT: Flight Inventory]
│   ├── application/
│   │   └── SeatReservationService  [Seat allocation orchestration]
│   │
│   ├── domain/
│   │   └── FlightService           [Flight lookup & availability queries]
│   │
│   ├── capacity/
│   │   ├── FlightCapacityStrategy  [Strategy pattern for capacity validation]
│   │   ├── StandardCapacityStrategy
│   │   ├── OverbookableCapacityStrategy
│   │   └── DiscountedCapacityStrategy
│   │
│   ├── monitoring/
│   │   └── FlightAvailabilityLogger [Observability: logs capacity changes]
│   │
│   └── registry/
│       ├── FlightRegistry          [Thread-safe inventory management]
│       ├── RegisteredFlight        [Flight entity with synchronized mutations]
│       └── FlightInventoryLoader   [JSON deserialization & validation]
│
├── config/
│   └── BookingConfig               [@ConfigurationProperties for runtime policies]
│
├── domain/                         [Shared domain models]
│   ├── BookingRequest              [Request DTO for POST /api/bookings]
│   ├── BookingResponse             [Response DTO with booking details]
│   ├── BookingAuditEntry           [Audit log entry record]
│   └── FlightAvailabilityResponse  [Response DTO for GET /api/flights/{id}/availability]
│
└── exception/                      [Custom exception hierarchy]
    ├── FlightNotFoundException
    ├── BookingNotFoundException
    ├── OverbookingException
    └── BookingPolicyViolationException
```

### Design Rationale

| Package | Responsibility | Why? |
|---------|-----------------|------|
| `api/` | HTTP request/response handling | Isolate Spring Web concerns from business logic |
| `application/` | Workflow orchestration (Facades) | Coordinate multiple domain services |
| `domain/` | Core business logic & persistence | Single Responsibility: each service owns one concern |
| `capacity/` | Strategy pattern for booking rules | Allow different overbooking strategies without changing core logic |
| `registry/` | Thread-safe inventory access | Centralize concurrency control for flight state |
| `monitoring/` | Observability & logging hooks | Separate operational concerns from business logic |
| `config/` | Externalized configuration | Runtime policy changes without recompilation |
| `exception/` | Custom exceptions | Semantic error handling, better API contracts |

---

## 2. Request Lifecycle: Controller → Service → Storage

### Booking Workflow (`POST /api/bookings`)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. HTTP Request Arrives                                     │
│    POST /api/bookings with BookingRequest (JSON)            │
└────────────┬────────────────────────────────────────────────┘
             ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. BookingController.createBooking()                        │
│    • @Valid annotation triggers JSR-303 validation          │
│    • If invalid: 400 Bad Request (fieldErrors)              │
│    • If valid: Continue to step 3                           │
└────────────┬────────────────────────────────────────────────┘
             ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. BookingManagementService.bookTicket() [FACADE]           │
│                                                             │
│    3a. Normalize flightNumber (trim, uppercase)             │
│    3b. Validate passenger's seat limit via BookingConfig    │
│        - If exceeds: throw BookingPolicyViolationException  │
│                                                             │
│    3c. Verify flight exists (call FlightService)            │
│        - If not found: throw FlightNotFoundException (404)  │
│                                                             │
│    3d. Reserve seats (call SeatReservationService)          │
│        - Validates availability against capacity            │
│        - If overbooking: throw OverbookingException (409)   │
│        - Mutates RegisteredFlight.bookedSeats               │
│                                                             │
│    3e. Persist booking record (call BookingService)         │
│        - Generates unique bookingId                         │
│        - Stores in ConcurrentHashMap                        │
│                                                             │
│    3f. Log to audit trail (call BookingAuditLog)            │
│        - Record: booking ID, passenger, flight, seats, time │
│                                                             │
│    3g. Log flight availability update                       │
│        - FlightAvailabilityLogger prints capacity snapshot  │
└────────────┬────────────────────────────────────────────────┘
             ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Return BookingResponse                                   │
│    HTTP 201 Created                                         │
│    Location: /api/bookings/1                                │
│    Body: { bookingId, flightNumber, passengerName, seats }  │
└─────────────────────────────────────────────────────────────┘
```

### Availability Query Workflow (`GET /api/flights/{flightNumber}/availability`)

```
BookingController.getFlightAvailability()
    ▼
BookingManagementService.getFlightAvailability()
    ▼
FlightService.getFlightAvailability()
    ▼
FlightRegistry.findFlight(flightNumber)
    ▼ (Thread-safe read via AtomicReference)
RegisteredFlight.toAvailabilityResponse()
    ▼
Return FlightAvailabilityResponse (200 OK)
```

### Cancellation Workflow (`DELETE /api/bookings/{bookingId}`)

```
BookingController.cancelBooking()
    ▼
BookingManagementService.cancelBooking()
    │
    ├─→ 1. BookingService.getBookingById(bookingId)
    │       If not found: throw BookingNotFoundException (404)
    │
    ├─→ 2. Validate cancellation policy
    │       BookingService.registerCancellation()
    │       If already cancelled once: throw BookingPolicyViolationException (409)
    │
    ├─→ 3. SeatReservationService.releaseSeats()
    │       Mutates RegisteredFlight.bookedSeats (decrement)
    │
    ├─→ 4. BookingService.deleteBooking(bookingId)
    │       Remove from ConcurrentHashMap
    │
    ├─→ 5. BookingAuditLog.logCancellation()
    │       Record cancellation event
    │
    └─→ 6. FlightAvailabilityLogger.logFlightAvailability()
            Print updated capacity snapshot
    ▼
Return 204 No Content (no body, just headers)
```

### Data Flow Summary

```
Request
  ↓
Spring Validation (JSR-303)
  ↓
BookingController (HTTP endpoint)
  ↓
BookingManagementService (Orchestration)
  ├→ FlightService (lookup flight)
  ├→ SeatReservationService (allocate seats)
  ├→ BookingService (persist booking)
  ├→ BookingAuditLog (compliance trail)
  └→ FlightAvailabilityLogger (observability)
  ↓
Response (201/200/204/400/404/409)
```

---

## 3. Concurrency and Overbooking Prevention Strategy

### The Problem: Race Conditions in Multi-Threaded Booking

In a flight with 2 available seats, two concurrent threads attempt to book 2 seats each:

```
Thread A: Read available=2, reserve 2 seats
Thread B: Read available=2 (didn't see A's change yet), reserve 2 seats
Result: 4 seats booked in a flight with capacity 2 ❌ OVERBOOKING!
```

### Solution 1: Synchronized Methods on `RegisteredFlight`

```java
public class RegisteredFlight {
    private int bookedSeats;  // ← Not atomic, protected by synchronized
    
    public synchronized void reserveSeats(int requestedSeats) {
        if (bookedSeats + requestedSeats > capacity) {
            throw new OverbookingException(...);
        }
        bookedSeats += requestedSeats;  // ← Atomic mutation
    }
    
    public synchronized int getAvailableSeats() {
        return Math.max(0, capacity - bookedSeats);  // ← Consistent read
    }
}
```

**Why Synchronized?**
- Simple mutual exclusion: Only one thread can execute `reserveSeats()` at a time
- Prevents race conditions in the critical section (check + update)
- Built-in to Java, no external dependencies

### Solution 2: Thread-Safe Registry with `AtomicReference`

```java
public class FlightRegistry {
    private final AtomicReference<Map<String, RegisteredFlight>> flightsRef =
            new AtomicReference<>(Collections.emptyMap());
    
    public RegisteredFlight findFlight(String flightNumber) {
        return flightsRef.get().get(normalize(flightNumber));  // ← Atomic read
    }
    
    public synchronized void reload(Collection<FlightInventoryEntry> definitions) {
        // ← Synchronized to prevent race with concurrent reads/writes
        Map<String, RegisteredFlight> reloadedFlights = new LinkedHashMap<>();
        // ... build reloadedFlights ...
        flightsRef.set(Collections.unmodifiableMap(reloadedFlights));  // ← Atomic write
    }
}
```

**Why `AtomicReference`?**
- Allows atomic swap of entire flight inventory during reload
- Preserves booked seat counts across reloads
- Reads don't block; writers use synchronized method

### Solution 3: Immutable, Unmodifiable Maps

```java
flightsRef.set(Collections.unmodifiableMap(reloadedFlights));
```

**Why Immutable?**
- Once published, the map cannot be modified
- Other threads always read a consistent snapshot
- Prevents accidental mutations outside of synchronized blocks

### Concurrency Test: Proof of Safety

```java
@Test
void testConcurrentBookingSafety() {
    RegisteredFlight flight = new RegisteredFlight("TB300", 2);
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    // 10 threads, each trying to book 2 seats
    for (int i = 0; i < 10; i++) {
        executor.submit(() -> {
            try {
                flight.reserveSeats(2);  // Will throw for most threads
            } catch (OverbookingException e) {
                // Expected for 8 out of 10 threads
            }
        });
    }
    
    executor.awaitTermination(5, TimeUnit.SECONDS);
    assertEquals(2, flight.getBookedSeats());  // ✅ Exactly 2, not 4, 6, 8, or 10
}
```

**Result:** Only 1 thread succeeds (gets 2 seats before limit is reached). The other 9 threads fail with `OverbookingException`. No overbooking occurs.

### Memory Visibility Guarantees

| Operation | Thread-Safety | Guarantee |
|-----------|---------------|-----------|
| `RegisteredFlight.reserveSeats()` | `synchronized` | Mutual exclusion; volatile semantics |
| `RegisteredFlight.getBookedSeats()` | `synchronized` | Consistent read after acquire lock |
| `FlightRegistry.findFlight()` | `AtomicReference.get()` | Happen-before edge; sees latest state |
| `FlightRegistry.reload()` | `synchronized` | Atomic swap; all threads see new map |

---

## 4. In-Memory/JSON Registry Design and Reload Behavior

### Flight Registry: The Source of Truth

```
startup
  ↓
FlightInventoryLoader.loadInventory() reads flights.json
  ↓
FlightRegistry.initialize() via @PostConstruct
  ↓
FlightRegistry.reloadFromConfig()
  ↓
reloadedFlights Map built in memory
  ↓
AtomicReference swapped to new unmodifiable map
  ↓
All bookings use RegisteredFlight objects from registry
```

### State Preservation During Reload

One challenge: How to reload flight definitions without losing existing bookings?

**Solution:**

```java
public synchronized void reload(Collection<FlightInventoryEntry> definitions) {
    Map<String, RegisteredFlight> currentFlights = flightsRef.get();  // ← Old state
    Map<String, RegisteredFlight> reloadedFlights = new LinkedHashMap<>();
    
    for (FlightInventoryEntry definition : definitions) {
        String normalizedFlightNumber = normalize(definition.flightNumber());
        
        // ← Preserve booked seats from existing flight
        RegisteredFlight existingFlight = currentFlights.get(normalizedFlightNumber);
        int bookedSeats = existingFlight == null ? 0 : existingFlight.getBookedSeats();
        
        // Create new RegisteredFlight with old booking count
        reloadedFlights.put(
            normalizedFlightNumber,
            new RegisteredFlight(normalizedFlightNumber, definition.capacity(), bookedSeats)
        );
    }
    
    flightsRef.set(Collections.unmodifiableMap(reloadedFlights));  // ← Atomic swap
}
```

**Example:**

Before reload:
```
TB100: capacity=5, bookedSeats=3
TB200: capacity=3, bookedSeats=1
```

Reload with new flights.json (TB200 removed, TB100 capacity increased):
```
[
  {"flightNumber": "TB100", "capacity": 10},  // ← Changed
  {"flightNumber": "TB300", "capacity": 4}    // ← New
]
```

After reload:
```
TB100: capacity=10, bookedSeats=3  ← Capacity increased, bookings preserved!
TB200: removed                      ← No longer in inventory
TB300: capacity=4, bookedSeats=0    ← New flight, no bookings
```

### JSON Inventory Loader: Validation at Parse Time

```java
public Collection<FlightInventoryEntry> loadInventory() {
    Collection<FlightInventoryEntry> entries = objectMapper.readValue(inputStream, 
        new TypeReference<Collection<FlightInventoryEntry>>(){});
    
    Set<String> seen = new HashSet<>();
    for (FlightInventoryEntry entry : entries) {
        if (entry.flightNumber() == null || entry.flightNumber().isBlank()) {
            throw new IllegalStateException("Flight number must not be blank");
        }
        
        String normalized = entry.flightNumber().trim().toUpperCase(Locale.ROOT);
        if (seen.contains(normalized)) {
            throw new IllegalStateException("Duplicate flight number: " + normalized);
        }
        seen.add(normalized);
        
        if (entry.capacity() < 1) {
            throw new IllegalStateException("Flight capacity must be at least 1");
        }
    }
    
    return entries;
}
```

**Validation Rules:**
- ✅ Non-blank flight numbers
- ✅ No duplicate flight numbers (case-insensitive)
- ✅ Capacity >= 1
- ✅ If validation fails: `IllegalStateException` at startup (fail-fast)

---

## 5. Design Decisions and Why They Were Chosen

### Decision 1: Synchronized Methods vs. Locks

**Chosen: Synchronized methods on `RegisteredFlight`**

```java
public synchronized void reserveSeats(int requestedSeats) { ... }
```

**Alternatives Considered:**
- ❌ **ReentrantLock**: More flexible but adds complexity; synchronized sufficient
- ❌ **Atomic variables**: Only good for single fields; we need multi-step transactions
- ✅ **Synchronized**: Simple, clear, built-in to Java; sufficient for single-flight ops

**Why?**
- KISS (Keep It Simple): 2-3 lines of code vs. 10+ with explicit locks
- No performance penalty for single-node deployment
- Clear intent: "this method is a critical section"

---

### Decision 2: Spring `@ConfigurationProperties` for Policies

**Chosen: `BookingConfig` + validated properties**

```java
@Validated
@ConfigurationProperties(prefix = "ticketbook.booking")
public class BookingConfig {
    @Min(value = 1)
    private int maxSeatsPerPassenger = 5;
}
```

**Alternatives Considered:**
- ❌ **Hardcoded constants**: Not customizable; requires code change to adjust
- ❌ **Custom annotation**: More verbose; duplicates Spring Config functionality
- ✅ **@ConfigurationProperties**: Standard Spring pattern, type-safe, validated

**Why?**
- Configuration managed externally (application.properties)
- Runtime adjustments without recompilation
- Type-safe bean injection into services
- JSR-303 validation at startup (fail-fast on bad config)

---

### Decision 3: Strategy Pattern for Capacity Validation

**Chosen: `FlightCapacityStrategy` interface**

```java
public interface FlightCapacityStrategy {
    int maxBookableSeats(int baseCapacity);
    void validateReservation(RegisteredFlight flight, int requestedSeats);
    String strategyName();
}
```

**Alternatives Considered:**
- ❌ **Hard-coded if/else**: Not extensible; violates Open/Closed Principle
- ❌ **Enums**: Difficult to add new strategies; couples logic to enum
- ✅ **Strategy pattern**: New strategies pluggable via `@Component`

**Why?**
- Different business rules for different flight types (standard, overbookable, discounted)
- Easy to add new strategies without modifying existing code
- Testable: mock different strategies in isolation
- Spring auto-discovers strategies via component scanning

---

### Decision 4: Facade Pattern for Booking Orchestration

**Chosen: `BookingManagementService` as facade**

```java
@Service
public class BookingManagementService {
    private final BookingService bookingService;
    private final FlightService flightService;
    private final SeatReservationService seatReservationService;
    private final BookingAuditLog auditLog;
    // ... coordinates all of these
}
```

**Alternatives Considered:**
- ❌ **No facade**: Calls scattered across many services; hard to follow workflow
- ❌ **God service**: Single giant service; violates Single Responsibility
- ✅ **Facade**: Orchestrates workflow; delegates to specialized services

**Why?**
- Clear workflow visible in one place (booking → seat reservation → audit)
- Decouples API from implementation (can swap implementations)
- Testable: mock each collaborator
- Standard architectural pattern for multi-step workflows

---

### Decision 5: In-Memory Storage with JSON Source

**Chosen: No database; flights.json + in-memory registry**

```
flights.json → FlightInventoryLoader → FlightRegistry → RegisteredFlight objects
```

**Alternatives Considered:**
- ❌ **Database (PostgreSQL)**: Overkill for demo; adds complexity, schema migration
- ❌ **Properties file**: Harder to validate; no structure enforcement
- ✅ **JSON file**: Structured, validated at parse time; easy to inspect

**Why?**
- Matches project scope: "In-memory storage only"
- JSON validation at startup prevents silent failures
- Flights.json is self-documenting (version control friendly)
- Easy to mock in tests (ByteArrayResource)

---

### Decision 6: Unmodifiable Maps After Registry Reload

**Chosen: `Collections.unmodifiableMap()`**

```java
flightsRef.set(Collections.unmodifiableMap(reloadedFlights));
```

**Alternatives Considered:**
- ❌ **Mutable map + warnings**: Relies on discipline; easy to misuse
- ❌ **Copy on read**: Extra allocation overhead
- ✅ **Unmodifiable after set**: Programmatic enforcement; fail-fast on mutation

**Why?**
- Prevents accidental mutations outside of reload
- Thread-safe by design: immutable objects don't need synchronization for reads
- Clear API: "read-only after this point"

---

### Decision 7: Booking Policy Enforcement at Service Layer

**Chosen: Validation in `BookingManagementService.bookTicket()`**

```java
public BookingResponse bookTicket(BookingRequest request) {
    if (seatsToBook > bookingConfig.getMaxSeatsPerPassenger()) {
        throw new BookingPolicyViolationException(...);
    }
    // ... proceed with booking
}
```

**Alternatives Considered:**
- ❌ **Controller validation**: Mixes HTTP concerns with business logic
- ❌ **Database constraint**: Would require persistence; not available
- ✅ **Service layer**: Business logic at the right level; testable

**Why?**
- Service layer is the right place for business rules
- Consistent enforcement regardless of caller (HTTP or internal)
- Testable without mocking HTTP infrastructure
- Clear separation: Controller validates syntax, Service validates semantics

---

### Decision 8: Separate Booking Service and Booking Management Service

**Chosen: `BookingService` (persistence) + `BookingManagementService` (orchestration)**

```
BookingService: CRUD operations on booking records
  └─ bookingMap.put(id, record)
  └─ bookingMap.get(id)
  └─ bookingMap.remove(id)

BookingManagementService: Coordinates full workflow
  └─ Calls BookingService + SeatReservationService + Audit
```

**Why?**
- **Single Responsibility**: Each service owns one concern
- **Testability**: Can test persistence logic independently from workflow
- **Reusability**: `BookingService` can be called by multiple workflows (refund, hold, etc.)
- **Clarity**: Clear distinction between "what to store" vs. "how to book"

---

## Summary: Architecture Principles Applied

| Principle | Application |
|-----------|-------------|
| **DDD** | Bounded contexts (booking, flight); clear domain model |
| **Single Responsibility** | Each service owns one concern |
| **Open/Closed** | Strategy pattern allows new behaviors without modification |
| **Dependency Injection** | Spring autowiring; loosely coupled components |
| **Immutability** | Unmodifiable maps, records for domain models |
| **Thread-Safety** | Synchronized methods, AtomicReference, immutable snapshots |
| **Fail-Fast** | Validation at startup (configuration, JSON); exceptions in workflows |
| **Observability** | Structured logging, audit trail, flight availability snapshots |

---

## Deployment & Observability

### Logging

The system logs at key decision points:

```
[INFO] Flight availability at startup:
  Flight TB100: 5 total, 0 booked, 5 available
  Flight TB200: 3 total, 0 booked, 3 available

[INFO] Processing booking request for passenger: Alice on flight: TB100
[INFO] Booking created with ID: 1 and audit logged
[INFO] Flight availability after booking 1:
  Flight TB100: 5 total, 2 booked, 3 available
```

### Audit Trail

Bookings are logged to `BookingAuditLog`:

```java
bookingId=1, passenger=Alice, flight=TB100, seats=2, action=BOOKING, timestamp=...
bookingId=1, passenger=Alice, flight=TB100, seats=2, action=CANCELLATION, timestamp=...
```

(In-memory only; lost on restart. For production, would migrate to database.)

---

## Future Enhancements

See [README.md § 9](./README.md#9-if-i-had-more-time-roadmap) for roadmap.

Key architectural changes needed:
1. **Persistence**: Replace `ConcurrentHashMap` with `@Entity` JPA objects
2. **Distributed Locking**: Add Redis for multi-node deployments
3. **Event Sourcing**: Replay booking events for audit compliance
4. **Caching**: Layer Redis cache in front of flight registry
