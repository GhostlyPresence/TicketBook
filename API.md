# TicketBook API Reference

Complete endpoint contract documentation with request/response examples, error scenarios, and business rules.

---

## 1. Endpoint-by-Endpoint Contract

### Endpoint 1: Create Booking

**Endpoint:** `POST /api/bookings`

**Responsibility:** Book seats on a flight for a passenger.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body (BookingRequest):**
```json
{
  "flightNumber": "TB100",
  "passengerName": "Alice Johnson",
  "seats": 2
}
```

**Request Field Validation:**
| Field | Type | Required | Rules | Error Code |
|-------|------|----------|-------|-----------|
| `flightNumber` | string | ✅ Yes | Non-blank; max 20 chars | 400 Bad Request |
| `passengerName` | string | ✅ Yes | Non-blank; max 100 chars | 400 Bad Request |
| `seats` | integer | ❌ No | Min: 1; Max: config value | 400 Bad Request |

**Default Behavior:**
- If `seats` omitted: defaults to `1`

**Success Response:**

**Status Code:** `201 Created`

**Response Headers:**
```
Location: /api/bookings/1
Content-Type: application/json
```

**Response Body (BookingResponse):**
```json
{
  "bookingId": 1,
  "flightNumber": "TB100",
  "passengerName": "Alice Johnson",
  "seats": 2
}
```

**Response Field Details:**
| Field | Type | Description |
|-------|------|-------------|
| `bookingId` | long | Unique booking identifier; auto-generated starting from 1 |
| `flightNumber` | string | Flight code in uppercase (normalized from request) |
| `passengerName` | string | Passenger name as provided in request |
| `seats` | int | Number of seats booked (defaults to 1 if omitted) |

---

### Endpoint 2: Get Flight Availability

**Endpoint:** `GET /api/flights/{flightNumber}/availability`

**Responsibility:** Query current seat availability for a flight.

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `flightNumber` | string | ✅ Yes | Flight code (case-insensitive; e.g., `TB100`) |

**Query Parameters:** None

**Success Response:**

**Status Code:** `200 OK`

**Response Body (FlightAvailabilityResponse):**
```json
{
  "flightNumber": "TB100",
  "totalCapacity": 5,
  "bookedSeats": 2,
  "availableSeats": 3
}
```

**Response Field Details:**
| Field | Type | Description | Calculation |
|-------|------|-------------|-------------|
| `flightNumber` | string | Flight code in uppercase | Normalized from request parameter |
| `totalCapacity` | int | Total seats on the flight | From flights.json |
| `bookedSeats` | int | Currently reserved seats | Sum of all active bookings |
| `availableSeats` | int | Remaining seats for booking | `totalCapacity - bookedSeats` |

---

### Endpoint 3: Cancel Booking

**Endpoint:** `DELETE /api/bookings/{bookingId}`

**Responsibility:** Cancel an existing booking and release seats back to the flight.

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `bookingId` | long | ✅ Yes | Booking identifier from POST response |

**Query Parameters:** None

**Request Body:** None

**Success Response:**

**Status Code:** `204 No Content`

**Response Headers:**
```
Content-Type: application/json (or empty)
```

**Response Body:** Empty (no content for 204)

**Side Effects:**
- Booking record deleted from in-memory registry
- Seats released back to flight (booked count decremented)
- Audit trail updated with cancellation event
- Flight availability snapshot logged to stdout

---

## 2. Request/Response Examples

### Example 1: Basic Booking (Happy Path)

**Request:**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Alice Johnson",
    "seats": 2
  }'
```

**Response:**
```
HTTP/1.1 201 Created
Location: /api/bookings/1
Content-Type: application/json
Content-Length: 78
Date: Thu, 17 Apr 2026 10:30:45 GMT

{
  "bookingId": 1,
  "flightNumber": "TB100",
  "passengerName": "Alice Johnson",
  "seats": 2
}
```

---

### Example 2: Booking with Default Seat Count

**Request (seats field omitted):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB200",
    "passengerName": "Bob Smith"
  }'
```

**Response:**
```
HTTP/1.1 201 Created
Location: /api/bookings/2

{
  "bookingId": 2,
  "flightNumber": "TB200",
  "passengerName": "Bob Smith",
  "seats": 1
}
```

---

### Example 3: Multiple Bookings (Sequential)

**Request 1:**
```bash
curl -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB300",
    "passengerName": "Charlie",
    "seats": 1
  }'
```

**Response 1:**
```json
{
  "bookingId": 3,
  "flightNumber": "TB300",
  "passengerName": "Charlie",
  "seats": 1
}
```

Now TB300 has 1/2 seats available.

**Request 2 (same flight):**
```bash
curl -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB300",
    "passengerName": "Diana",
    "seats": 1
  }'
```

**Response 2:**
```json
{
  "bookingId": 4,
  "flightNumber": "TB300",
  "passengerName": "Diana",
  "seats": 1
}
```

Now TB300 is fully booked (2/2 seats).

---

### Example 4: Flight Availability Query

**Request:**
```bash
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "flightNumber": "TB100",
  "totalCapacity": 5,
  "bookedSeats": 2,
  "availableSeats": 3
}
```

---

### Example 5: Cancellation

**Request:**
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/1
```

**Response:**
```
HTTP/1.1 204 No Content
Date: Thu, 17 Apr 2026 10:31:00 GMT
```

After cancellation, TB100 availability:
```bash
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

```json
{
  "flightNumber": "TB100",
  "totalCapacity": 5,
  "bookedSeats": 0,
  "availableSeats": 5
}
```

Seats freed up: 2 seats released back to the flight.

---

## 3. Failure Scenarios with Example Responses

### Scenario 1: Flight Not Found

**Request:**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "XX999",
    "passengerName": "Eve",
    "seats": 1
  }'
```

**Response:**
```
HTTP/1.1 404 Not Found
Content-Type: application/json
Date: Thu, 17 Apr 2026 10:32:00 GMT

{
  "timestamp": "2026-04-17T10:32:00.123456Z",
  "status": 404,
  "error": "Not Found",
  "message": "Flight not found: XX999"
}
```

**Root Cause:** Flight `XX999` does not exist in flights.json.

---

### Scenario 2: Overbooking Attempt (Insufficient Seats)

**Request (TB300 has only 2 total seats):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB300",
    "passengerName": "Frank",
    "seats": 3
  }'
```

**Response:**
```
HTTP/1.1 409 Conflict
Content-Type: application/json
Date: Thu, 17 Apr 2026 10:33:00 GMT

{
  "timestamp": "2026-04-17T10:33:00.234567Z",
  "status": 409,
  "error": "Conflict",
  "message": "Not enough seats available on flight TB300 (3 requested, 2 available)"
}
```

**Root Cause:** Requested 3 seats but flight has only 2 available.

---

### Scenario 3: Passenger Booking Limit Exceeded

**Request (max-seats-per-passenger configured as 5):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Grace",
    "seats": 6
  }'
```

**Response:**
```
HTTP/1.1 409 Conflict
Content-Type: application/json
Date: Thu, 17 Apr 2026 10:34:00 GMT

{
  "timestamp": "2026-04-17T10:34:00.345678Z",
  "status": 409,
  "error": "Conflict",
  "message": "A passenger can book at most 5 seat(s)"
}
```

**Root Cause:** Policy limit exceeded; configuration enforced at service layer.

---

### Scenario 4: Cancellation Limit Exceeded

**Scenario:** max-cancellations-per-booking = 1 (allow one cancellation)

**First cancellation (succeeds):**
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/1
```

```
HTTP/1.1 204 No Content
```

**Second cancellation (fails):**
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/1
```

```
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "timestamp": "2026-04-17T10:35:00.456789Z",
  "status": 409,
  "error": "Conflict",
  "message": "Booking 1 has already been cancelled"
}
```

**Root Cause:** Policy limit: one cancellation per booking allowed; second attempt violates policy.

---

### Scenario 5: Booking Not Found

**Request (bookinID doesn't exist):**
```bash
curl -i -X DELETE http://localhost:1234/api/bookings/99999
```

**Response:**
```
HTTP/1.1 404 Not Found
Content-Type: application/json
Date: Thu, 17 Apr 2026 10:36:00 GMT

{
  "timestamp": "2026-04-17T10:36:00.567890Z",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found: 99999"
}
```

**Root Cause:** Booking ID does not exist.

---

### Scenario 6: Missing Required Field

**Request (flightNumber omitted):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "passengerName": "Henry",
    "seats": 1
  }'
```

**Response:**
```
HTTP/1.1 400 Bad Request
Content-Type: application/json
Date: Thu, 17 Apr 2026 10:37:00 GMT

{
  "timestamp": "2026-04-17T10:37:00.678901Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "flightNumber": "flightNumber is required"
  }
}
```

**Root Cause:** JSR-303 validation failed; field marked `@NotBlank`.

---

### Scenario 7: Invalid Seat Count

**Request (seats < 1):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Ivy",
    "seats": 0
  }'
```

**Response:**
```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2026-04-17T10:38:00.789012Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "seats": "seats must be at least 1"
  }
}
```

**Root Cause:** JSR-303 validation failed; `@Min(1)` constraint violated.

---

### Scenario 8: Malformed JSON

**Request (invalid JSON syntax):**
```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Jack"
    "seats": 1
  }'  # ← Missing comma before "seats"
```

**Response:**
```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2026-04-17T10:39:00.890123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "...(JSON parse error details)..."
}
```

**Root Cause:** JSON deserialization failed; Spring's default error handler responds with 400.

---

## 4. Idempotency and Consistency Notes

### Idempotency Status: **NOT IMPLEMENTED**

**Current Limitation:** The API does not support idempotency keys. 

**Scenario: Retry Risk**

```bash
# Request 1: Network timeout, client retries
curl -X POST http://localhost:1234/api/bookings \
  -d '{"flightNumber":"TB100","passengerName":"Kate","seats":1}'

# Server processed, but response lost
# Client receives timeout error

# Request 2: Automatic retry (or user retry)
curl -X POST http://localhost:1234/api/bookings \
  -d '{"flightNumber":"TB100","passengerName":"Kate","seats":1}'

# Server processes again
# Duplicate booking created! bookingId=5 and bookingId=6
```

**Workaround:** Clients should validate response status before retrying. If timeout, check availability:
```bash
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

If seats are reduced, assume booking succeeded.

### Consistency Guarantees: **In-Memory Only**

**Strong Consistency:** All operations within a single process are immediately consistent:

```bash
# After booking succeeds
curl -X GET http://localhost:1234/api/flights/TB100/availability
# Immediately reflects new booked count ✅
```

**Weak Consistency on Restart:** In-memory state is lost:

```bash
# Before restart: Booking 1, TB100 has 2 booked seats
mvn spring-boot:run
# After restart: TB100 has 0 booked seats (clean slate) ⚠️
```

### Concurrent Consistency: **Thread-Safe Across Threads**

```
Thread A books 1 seat ─┐
                       ├─→ Serialized via synchronized { } block
Thread B books 1 seat ─┘
```

Result: Both succeed, but booking counts are always consistent (no race conditions).

---

## 5. Error Response Reference

All error responses follow this standard structure:

```json
{
  "timestamp": "ISO8601 timestamp",
  "status": "HTTP status code",
  "error": "HTTP error reason phrase",
  "message": "Business logic error message",
  "fieldErrors": {  // ← Only for 400 Bad Request with validation failures
    "fieldName": "validation message"
  }
}
```

### Status Code Reference

| Code | Meaning | When | Example |
|------|---------|------|---------|
| 200 | OK | Availability query successful | `GET /api/flights/{id}/availability` |
| 201 | Created | Booking successful | `POST /api/bookings` → new record created |
| 204 | No Content | Cancellation successful | `DELETE /api/bookings/{id}` → record deleted |
| 400 | Bad Request | Invalid input (validation failed) | Missing field, negative seat count |
| 404 | Not Found | Resource doesn't exist | Flight ID or booking ID not found |
| 409 | Conflict | Business rule violated | Overbooking, policy limit, duplicate cancel |
| 500 | Server Error | Unexpected runtime error | Not expected in normal operation |

### Exception-to-Status Mapping

| Java Exception | HTTP Status | Cause |
|---|---|---|
| `FlightNotFoundException` | 404 | Flight ID from request doesn't exist |
| `BookingNotFoundException` | 404 | Booking ID from request doesn't exist |
| `OverbookingException` | 409 | Not enough available seats |
| `BookingPolicyViolationException` | 409 | Passenger limit, cancellation limit, etc. |
| `MethodArgumentNotValidException` | 400 | Request validation failed (JSR-303) |

---

## 6. Capacity by Flight (Pre-Configured)

| Flight | Total Seats | Use Case |
|--------|-------------|----------|
| TB100 | 5 | Medium flight |
| TB200 | 3 | Small flight |
| TB300 | 2 | Very limited flight (easy to fill) |

To modify, edit `src/main/resources/flights.json` and restart the service.

---

## 7. Configuration Parameters

Runtime behavior is controlled via `application.properties`:

```properties
# Port
server.port=1234

# Booking limits
ticketbook.booking.max-seats-per-passenger=5         # Max seats per booking
ticketbook.booking.max-cancellations-per-booking=1   # Max cancellations allowed
```

To test different policies, modify these values and restart.

---

## 8. Sample Client Patterns

### Pattern 1: Book and Verify

```bash
#!/bin/bash

# Book seats
RESPONSE=$(curl -s -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Leo",
    "seats": 2
  }')

BOOKING_ID=$(echo $RESPONSE | jq .bookingId)
echo "Booking ID: $BOOKING_ID"

# Verify flight availability updated
curl -s -X GET http://localhost:1234/api/flights/TB100/availability | jq .
```

### Pattern 2: Concurrent Bookings

```bash
#!/bin/bash

# Simulate 5 concurrent bookings on same flight
for i in {1..5}; do
  curl -X POST http://localhost:1234/api/bookings \
    -H "Content-Type: application/json" \
    -d "{
      \"flightNumber\": \"TB100\",
      \"passengerName\": \"Passenger$i\",
      \"seats\": 1
    }" &
done

wait  # Wait for all to complete

# Check final state
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

### Pattern 3: Graceful Failure Handling

```bash
#!/bin/bash

# Try to book; handle errors gracefully
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB300",
    "passengerName": "Mary",
    "seats": 3
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -1)

if [ "$HTTP_CODE" -eq 201 ]; then
  echo "✅ Booking succeeded: $(echo $BODY | jq .bookingId)"
elif [ "$HTTP_CODE" -eq 409 ]; then
  echo "⚠️ Overbooking prevented: $(echo $BODY | jq .message)"
elif [ "$HTTP_CODE" -eq 404 ]; then
  echo "❌ Flight not found"
else
  echo "❌ Error: HTTP $HTTP_CODE"
fi
```

---

## Quick Reference: All Endpoints

| Method | Path | Purpose | Success Status |
|--------|------|---------|---|
| POST | `/api/bookings` | Create booking | 201 Created |
| GET | `/api/flights/{flightNumber}/availability` | Check availability | 200 OK |
| DELETE | `/api/bookings/{bookingId}` | Cancel booking | 204 No Content |

---

For architecture details, see [ARCHITECTURE.md](./ARCHITECTURE.md).

For setup and testing guidance, see [README.md](./README.md).
