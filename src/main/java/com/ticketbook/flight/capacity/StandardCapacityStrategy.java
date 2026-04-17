package com.ticketbook.flight.capacity;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class StandardCapacityStrategy implements FlightCapacityStrategy {

    @Override
    public int maxBookableSeats(int baseCapacity) {
        return baseCapacity;
    }

    @Override
    public String strategyName() {
        return "STANDARD";
    }
}
