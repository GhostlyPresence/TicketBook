package com.ticketbook.flight.capacity;

import com.ticketbook.exception.OverbookingException;
import com.ticketbook.flight.domain.FlightService;
import org.springframework.stereotype.Component;

@Component
public class DiscountedCapacityStrategy implements FlightCapacityStrategy {

    private static final double DISCOUNTED_INVENTORY_FACTOR = 0.80;
    private static final int MAX_SEATS_PER_BOOKING = 2;

    @Override
    public int maxBookableSeats(int baseCapacity) {
        return Math.max(1, (int) Math.floor(baseCapacity * DISCOUNTED_INVENTORY_FACTOR));
    }

    @Override
    public String strategyName() {
        return "DISCOUNTED";
    }

    @Override
    public void validateReservation(FlightService.Flight flight, int requestedSeats) {
        if (requestedSeats > MAX_SEATS_PER_BOOKING) {
            throw new OverbookingException(
                    "Discounted bookings allow up to " + MAX_SEATS_PER_BOOKING + " seat(s) per booking"
            );
        }

        FlightCapacityStrategy.super.validateReservation(flight, requestedSeats);
    }
}
