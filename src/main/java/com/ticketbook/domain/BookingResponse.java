package com.ticketbook.domain;

public record BookingResponse(
        long bookingId,
        String flightNumber,
        String passengerName,
        int seats
) {
}
