package com.ticketbook.flight.registry;

import com.ticketbook.domain.FlightAvailabilityResponse;

public class RegisteredFlight {

    private final String flightNumber;
    private final int capacity;
    private int bookedSeats;

    public RegisteredFlight(String flightNumber, int capacity) {
        this(flightNumber, capacity, 0);
    }

    public RegisteredFlight(String flightNumber, int capacity, int bookedSeats) {
        this.flightNumber = flightNumber;
        this.capacity = capacity;
        this.bookedSeats = Math.max(0, bookedSeats);
    }

    public synchronized void reserveSeats(int requestedSeats) {
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
        return Math.max(0, capacity - bookedSeats);
    }

    public synchronized FlightAvailabilityResponse toAvailabilityResponse() {
        return new FlightAvailabilityResponse(
                flightNumber,
                capacity,
                bookedSeats,
                Math.max(0, capacity - bookedSeats)
        );
    }
}
