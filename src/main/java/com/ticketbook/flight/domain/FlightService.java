package com.ticketbook.flight.domain;

import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.exception.FlightNotFoundException;
import com.ticketbook.flight.registry.FlightRegistry;
import com.ticketbook.flight.registry.RegisteredFlight;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Responsible for managing flights and their current state.
 * Single Responsibility: Flight inventory management
 */
@Service
public class FlightService {

    private final FlightRegistry flightRegistry;

    public FlightService(FlightRegistry flightRegistry) {
        this.flightRegistry = flightRegistry;
    }

    public RegisteredFlight getFlightByNumber(String flightNumber) {
        RegisteredFlight flight = flightRegistry.findFlight(flightNumber);

        if (flight == null) {
            throw new FlightNotFoundException("Flight " + flightNumber.trim().toUpperCase() + " was not found");
        }

        return flight;
    }

    public FlightAvailabilityResponse getFlightAvailability(String flightNumber) {
        RegisteredFlight flight = getFlightByNumber(flightNumber);
        return flight.toAvailabilityResponse();
    }

    public Collection<RegisteredFlight> getAllFlights() {
        return flightRegistry.getAllFlights();
    }
}
