package com.ticketbook.flight.capacity;

import com.ticketbook.exception.OverbookingException;
import com.ticketbook.flight.domain.FlightService;

public interface FlightCapacityStrategy {

    int maxBookableSeats(int baseCapacity);

    String strategyName();

    default void validateReservation(FlightService.Flight flight, int requestedSeats) {
        int maxSeats = maxBookableSeats(flight.getCapacity());
        int remainingSeats = maxSeats - flight.getBookedSeats();

        if (requestedSeats > remainingSeats) {
            throw new OverbookingException(
                    "Flight " + flight.getFlightNumber() + " has only " + Math.max(remainingSeats, 0)
                            + " seat(s) left under strategy " + strategyName()
            );
        }
    }
}
