package com.ticketbook.service;

import org.springframework.stereotype.Service;

/**
 * Responsible for managing seat reservations and releases.
 * Single Responsibility: Seat availability and reservation coordination
 */
@Service
public class SeatReservationService {

    private final FlightService flightService;

    public SeatReservationService(FlightService flightService) {
        this.flightService = flightService;
    }

    public void reserveSeats(String flightNumber, int seatsToReserve) {
        FlightService.Flight flight = flightService.getFlightByNumber(flightNumber);
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
