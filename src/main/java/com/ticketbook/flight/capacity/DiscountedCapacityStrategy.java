package com.ticketbook.flight.capacity;

import com.ticketbook.config.BookingConfig;
import com.ticketbook.exception.OverbookingException;
import com.ticketbook.flight.registry.RegisteredFlight;
import org.springframework.stereotype.Component;

@Component
public class DiscountedCapacityStrategy implements FlightCapacityStrategy {

    private static final double DISCOUNTED_INVENTORY_FACTOR = 0.80;
    private final BookingConfig bookingConfig;

    public DiscountedCapacityStrategy(BookingConfig bookingConfig) {
        this.bookingConfig = bookingConfig;
    }

    @Override
    public int maxBookableSeats(int baseCapacity) {
        return Math.max(1, (int) Math.floor(baseCapacity * DISCOUNTED_INVENTORY_FACTOR));
    }

    @Override
    public String strategyName() {
        return "DISCOUNTED";
    }

    @Override
    public void validateReservation(RegisteredFlight flight, int requestedSeats) {
        if (requestedSeats > bookingConfig.getMaxSeatsPerPassenger()) {
            throw new OverbookingException(
                    "Discounted bookings allow up to " + bookingConfig.getMaxSeatsPerPassenger() + " seat(s) per booking"
            );
        }

        FlightCapacityStrategy.super.validateReservation(flight, requestedSeats);
    }
}
