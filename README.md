# TicketBook Flight Booking API

A small Spring Boot REST API for in-memory flight ticket booking.

## Tech stack

- Java 17
- Spring Boot 3
- Maven
- In-memory storage only (no database)

## What is implemented

- `POST /api/bookings` to create a booking
- `GET /api/flights/{flightNumber}/availability` to check flight seat availability
- `DELETE /api/bookings/{bookingId}` to cancel a booking and free up seats
- No authentication/authorization
- No search endpoint (client provides `flightNumber` directly)
- Overbooking prevention with thread-safe reservation logic
- Comprehensive unit and integration tests (includes concurrent booking test)
- Proper HTTP status codes
  - `201 Created` for successful booking
  - `204 No Content` for successful cancellation
  - `200 OK` for availability queries
  - `400 Bad Request` for invalid input
  - `404 Not Found` when flight or booking number does not exist
  - `409 Conflict` when requested seats exceed available seats

The app is preloaded with these flights:

- `TB100` capacity: 5
- `TB200` capacity: 3
- `TB300` capacity: 2

## How to run

From the project root:

```bash
mvn spring-boot:run
```

Service starts on `http://localhost:1234`.

## Example requests

### 1) Successful booking

```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB100",
    "passengerName": "Alice",
    "seats": 2
  }'
```

Expected: `201 Created`

### 2) Flight not found

```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "UNKNOWN123",
    "passengerName": "Bob",
    "seats": 1
  }'
```

Expected: `404 Not Found`

### 3) Overbooking attempt

```bash
curl -i -X POST http://localhost:1234/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "TB300",
    "passengerName": "Charlie",
    "seats": 3
  }'
```

Expected: `409 Conflict`

### 4) Check flight availability

```bash
curl -X GET http://localhost:1234/api/flights/TB100/availability
```

Example response:
```json
{
  "flightNumber": "TB100",
  "totalCapacity": 5,
  "bookedSeats": 2,
  "availableSeats": 3
}
```

### 5) Check availability for flight with no space

```bash
curl -X GET http://localhost:1234/api/flights/TB300/availability
```

This endpoint shows:
- `flightNumber`: The flight identifier
- `totalCapacity`: Total seats on the flight
- `bookedSeats`: Currently booked seats
- `availableSeats`: Remaining available seats

### 6) Cancel a booking

```bash
curl -i -X DELETE http://localhost:1234/api/bookings/1
```

Expected: `204 No Content`

The booking is cancelled and seats are freed up for the flight.

### 7) Cancel non-existent booking

```bash
curl -i -X DELETE http://localhost:1234/api/bookings/99999
```

Expected: `404 Not Found`

## If I had more time

- Add automated tests (integration, including concurrent booking scenarios)
- Externalize flight inventory to configuration file and support dynamic reload
- Add idempotency key support to protect clients from duplicate retries
- Expose health/readiness endpoints and structured logging for operations
