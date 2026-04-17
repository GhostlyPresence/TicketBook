package com.ticketbook.flight.application;

import com.ticketbook.flight.capacity.FlightCapacityStrategy;
import com.ticketbook.flight.domain.FlightService;

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
        FlightService.Flight flight = flightService.getFlightByNumber(flightNumber);
        capacityStrategy.validateReservation(flight, seatsToReserve);
        flight.reserveSeats(seatsToReserve);
    }

    public void releaseSeats(String flightNumber, int seatsToRelease) {
        FlightService.Flight flight = flightService.getFlightByNumber(flightNumber);
        flight.releaseSeats(seatsToRelease);
    }

    public int getAvailableSeats(String flightNumber) {
        FlightService.Flight flight = flightService.getFlightByNumber(flightNumber);
        return flight.getAvailableSeats();
    }
}
