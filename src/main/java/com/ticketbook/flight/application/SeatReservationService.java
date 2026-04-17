package com.ticketbook.flight.application;

import com.ticketbook.flight.capacity.FlightCapacityStrategy;
import com.ticketbook.flight.domain.FlightService;
import com.ticketbook.flight.registry.RegisteredFlight;

import org.springframework.stereotype.Service;

/**
 * Responsible for managing seat reservations and releases.
 * Single Responsibility: Seat availability and reservation coordination
 */
@Service
public class SeatReservationService {

    private final FlightService flightService;
    private final FlightCapacityStrategy capacityStrategy;

    public SeatReservationService(FlightService flightService, FlightCapacityStrategy capacityStrategy) {
        this.flightService = flightService;
        this.capacityStrategy = capacityStrategy;
    }

    public void reserveSeats(String flightNumber, int seatsToReserve) {
        RegisteredFlight flight = flightService.getFlightByNumber(flightNumber);
        capacityStrategy.validateReservation(flight, seatsToReserve);
        flight.reserveSeats(seatsToReserve);
    }

    public void releaseSeats(String flightNumber, int seatsToRelease) {
        RegisteredFlight flight = flightService.getFlightByNumber(flightNumber);
        flight.releaseSeats(seatsToRelease);
    }

    public int getAvailableSeats(String flightNumber) {
        RegisteredFlight flight = flightService.getFlightByNumber(flightNumber);
        return flight.getAvailableSeats();
    }
}
