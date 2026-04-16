package com.ticketbook.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingRequest(
        @NotBlank(message = "flightNumber is required")
        String flightNumber,

        @NotBlank(message = "passengerName is required")
        String passengerName,

        @Min(value = 1, message = "seats must be at least 1")
        Integer seats
) {
}
