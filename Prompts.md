### Prompt 1: Base prompt to set the expectations
```
Design and implement a small REST API for a flight ticket booking system.
Technical expectations
- Use Spring Boot and Java
- Single application instance (no distributed systems concerns)
- No authentication, authorization, rate limiting
- No flight search or destination logic
- All booking operations assume the client already knows the flight number, no search required
- In-memory storage only (no database required)
- Do not allow overbooking the flights
- Do not need APIs to retrieve bookings. Only to book.
- Model REST endpoints as you see fit
- Use appropriate HTTP methods and status codes

Deliverables:
- Runnable Spring Boot project (Gradle or Maven)
- Short README.md including:
- how to run the service
- example requests
- what you would improve if you had more time
```
### Prompt 2: Enhance visibility inside APIs
```
since there is no db. print the flights name and available seats when the server starts and update it when someone books
```

### Prompt 3: Default port 8080 was having issues
```
change the port to 1234
```
### Prompt 4: Enhance reliability 
```
Add comprehensive unit tests
```
### Prompt 5: Add additional feature
```
Add flight availability endpoint and put it in the readme as well
```
### Prompt 6: Add additional feature
```
Add booking cancellation endpoint and put it in the readme as well
```
### Prompt 7: Creating an orchestrater layer
```
Create BookingManagementService as a facade that orchestrates BookingService + audit logging and add a BookingAuditLog entity (in-memory map) tracking: who booked, when, cancellations, refunds
```

### Prompt 8: Code clean up
```
the booking service is taking care of lot of stuff. Breaking the single responsibility principle. seperate out the responsibilities while also manintaing readability.
```

### Prompt 9: Implementing strategy pattern for overbooking logic
```
Create FlightCapacityStrategy interface with implementations:
StandardCapacityStrategy (current logic)
OverbookableCapacityStrategy (allow 110% for no-shows)
DiscountedCapacityStrategy (cheaper seats, different rules)

keep these as strategies but use the StandardCapacityStrategy by default
```
### Prompt 10: Reorganizing packages to avoid clutter
```
efactor the codebase structure to be production-ready by reorganizing packages around business domains and responsibilities.

Goals:

- Reduce package clutter and improve discoverability.
- Separate orchestration, core domain services, strategy implementations, audit concerns, and API adapters.
- Preserve existing behavior and public API contracts.
- Improve maintainability and onboarding clarity for future contributors.
- Restrictions:

Restrictions:

- Do not change runtime behavior, business rules, or endpoint contracts.
- Do not introduce new frameworks or architectural patterns beyond package/module reorganization.
- Keep class names and method signatures stable unless a change is strictly required for compilation.
- Update all imports, dependency wiring, and tests to keep the project fully buildable.
- Avoid broad formatting-only changes unrelated to the restructure.

Deliverables:

- A proposed target package structure with rationale.
- The applied file moves and code updates required to support the new structure.
- A short impact summary covering trade-offs, risks, and follow-up recommendations.
- Confirmation that the project compiles and tests pass after refactoring.
```

### Prompt 11: Removing hardcoded flight inventory and booking limits.

```
Act as a senior Spring Boot engineer and implement a production-grade Configuration + Registry pattern without changing existing API contracts.

Goals:

- Remove hardcoded flight inventory and booking limits.
- Centralize runtime rules in validated configuration.
- Keep booking/cancellation logic thread-safe and maintainable.

Outcomes:
- FlightRegistry loads flights from config, provides thread-safe access, and supports safe hot-reload.
- BookingConfig uses @ConfigurationProperties + validation for:
- maxSeatsPerPassenger
- maxCancellationsPerBooking
- Service layer uses config values instead of hardcoded limits.
- Endpoint paths, payloads, and status codes remain unchanged.
- Tests cover config binding, validation failures, registry reload, and limit enforcement.


Deliverables:
- Confirmation that the project compiles and tests pass after refactoring.
```

### Prompt 12: Move Flight inventory to json to simulate noSQL style storage
```
Move flight inventory from application.properties to a JSON file without changing API contracts.

Goals:

Flights come only from flights.json 
Keep booking/availability/cancellation behavior unchanged.

Do:

Add flights.json with flightNumber and capacity.
Load/validate it at startup (no duplicates, valid capacity).
Ensure all tests pass and endpoints/status codes remain the same.
```

### Prompt 13: Documentaion
```
Act as a senior backend engineer and create comprehensive, production-style documentation for this Spring Boot flight booking API project.

Documentation goals:
- Make the project easy to understand, run, test, and evaluate in a take-home review.
- Clearly explain architecture decisions, data flow, validation, concurrency handling, and tradeoffs.
- Show professional engineering quality, not just endpoint listing.

Deliverables:
- Rewrite README with:
1. Project overview and scope
2. Tech stack and prerequisites
3. Local setup and run steps
4. API quickstart with sample curl commands
5. Error handling model and status codes
6. Configuration guide (ports, limits, flight source)
7. Testing guide (what is covered and how to run)
8. Assumptions, constraints, and known limitations
9. “If I had more time” roadmap
- Add ARCHITECTURE.md with:
1. Folder/package structure and responsibilities
2. Request lifecycle from controller to service to storage
3. Concurrency and overbooking prevention strategy
4. In-memory/JSON registry design and reload behavior
5. Design decisions and why they were chosen
- Add API.md with:
1. Endpoint-by-endpoint contract
2. Request/response examples
3. Failure scenarios with example responses
4. Idempotency and consistency notes (if not implemented, state clearly)

Quality bar:
- Keep docs concise but complete, reviewer-friendly, and consistent with actual code.
- Do not invent features; mark non-implemented ideas explicitly.
- Include copy-paste-ready commands.
- Use clear headings, tables where useful, and practical examples.

Definition of done:
- A new reviewer should be able to clone, run, test, and understand design decisions in under 10 minutes.


```


### Conclusion
Beyond the base requirement of just a booking endpoint, I added:

1. Full test coverage with a concurrent booking test to prove overbooking prevention works under contention
2. Query and cancel endpoints to support the full booking lifecycle, not just creation
3. Structured logging so you can see seat availability changes in real-time
4. Proper error handling with custom exceptions and HTTP status code mapping
5. Clean layering between controller, service, and domain logic
This shows I think about testability, operations, and real-world concerns like concurrency and observability."