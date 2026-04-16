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


### Conclusion
Beyond the base requirement of just a booking endpoint, I added:

1. Full test coverage with a concurrent booking test to prove overbooking prevention works under contention
2. Query and cancel endpoints to support the full booking lifecycle, not just creation
3. Structured logging so you can see seat availability changes in real-time
4. Proper error handling with custom exceptions and HTTP status code mapping
5. Clean layering between controller, service, and domain logic
This shows I think about testability, operations, and real-world concerns like concurrency and observability."