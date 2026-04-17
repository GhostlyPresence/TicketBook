package com.ticketbook.booking.api;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.booking.application.BookingManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingManagementService bookingManagementService;

    public BookingController(BookingManagementService bookingManagementService) {
        this.bookingManagementService = bookingManagementService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingManagementService.bookTicket(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(booking.bookingId())
                .toUri();

        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping("/flights/{flightNumber}/availability")
    public ResponseEntity<FlightAvailabilityResponse> getFlightAvailability(@PathVariable String flightNumber) {
        FlightAvailabilityResponse availability = bookingManagementService.getFlightAvailability(flightNumber);
        return ResponseEntity.ok(availability);
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> cancelBooking(@PathVariable long bookingId) {
        bookingManagementService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}
