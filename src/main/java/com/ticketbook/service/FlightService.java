package com.ticketbook.service;

import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.exception.FlightNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for managing flights and their current state.
 * Single Responsibility: Flight inventory management
 */
@Service
public class FlightService {

    private final Map<String, Flight> flights = new ConcurrentHashMap<>();

    public FlightService() {
        flights.put("TB100", new Flight("TB100", 5));
        flights.put("TB200", new Flight("TB200", 3));
        flights.put("TB300", new Flight("TB300", 2));
    }

    public Flight getFlightByNumber(String flightNumber) {
        String normalizedFlightNumber = flightNumber.trim().toUpperCase();
        Flight flight = flights.get(normalizedFlightNumber);

        if (flight == null) {
            throw new FlightNotFoundException("Flight " + normalizedFlightNumber + " was not found");
        }

        return flight;
    }

    public FlightAvailabilityResponse getFlightAvailability(String flightNumber) {
        Flight flight = getFlightByNumber(flightNumber);
        return flight.toAvailabilityResponse();
    }

    public Map<String, Flight> getAllFlights() {
        return flights;
    }

    public static final class Flight {
        private final String flightNumber;
        private final int capacity;
        private int bookedSeats;

        public Flight(String flightNumber, int capacity) {
            this.flightNumber = flightNumber;
            this.capacity = capacity;
        }

        public synchronized void reserveSeats(int requestedSeats) {
            int remainingSeats = capacity - bookedSeats;
            if (requestedSeats > remainingSeats) {
                throw new com.ticketbook.exception.OverbookingException(
                        "Flight " + flightNumber + " has only " + remainingSeats + " seat(s) left"
                );
            }
            bookedSeats += requestedSeats;
        }

        public synchronized void releaseSeats(int seatsToRelease) {
            bookedSeats -= seatsToRelease;
            if (bookedSeats < 0) {
                bookedSeats = 0;
            }
        }

        public String getFlightNumber() {
            return flightNumber;
        }

        public int getCapacity() {
            return capacity;
        }

        public synchronized int getBookedSeats() {
            return bookedSeats;
        }

        public synchronized int getAvailableSeats() {
            return capacity - bookedSeats;
        }

        public synchronized FlightAvailabilityResponse toAvailabilityResponse() {
            return new FlightAvailabilityResponse(
                    flightNumber,
                    capacity,
                    bookedSeats,
                    capacity - bookedSeats
            );
        }
    }
}
