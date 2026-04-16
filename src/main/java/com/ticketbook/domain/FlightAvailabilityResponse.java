package com.ticketbook.domain;

public record FlightAvailabilityResponse(
        String flightNumber,
        int totalCapacity,
        int bookedSeats,
        int availableSeats
) {
}
