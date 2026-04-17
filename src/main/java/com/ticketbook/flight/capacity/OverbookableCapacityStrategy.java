package com.ticketbook.flight.capacity;

import org.springframework.stereotype.Component;

@Component
public class OverbookableCapacityStrategy implements FlightCapacityStrategy {

    private static final double OVERBOOK_FACTOR = 1.10;

    @Override
    public int maxBookableSeats(int baseCapacity) {
        return (int) Math.ceil(baseCapacity * OVERBOOK_FACTOR);
    }

    @Override
    public String strategyName() {
        return "OVERBOOKABLE";
    }
}
