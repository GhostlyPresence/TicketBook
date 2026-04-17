package com.ticketbook.service;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.exception.BookingNotFoundException;
import com.ticketbook.exception.FlightNotFoundException;
import com.ticketbook.exception.OverbookingException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final Map<String, FlightState> flights = new ConcurrentHashMap<>();
    private final AtomicLong bookingIdSequence = new AtomicLong(1);
    private final List<BookingResponse> bookings = new CopyOnWriteArrayList<>();

    public BookingService() {
        flights.put("TB100", new FlightState("TB100", 5));
        flights.put("TB200", new FlightState("TB200", 3));
        flights.put("TB300", new FlightState("TB300", 2));
    }

    @PostConstruct
    public void printFlightAvailabilityOnStartup() {
        logFlightAvailability("Flight availability at startup:");
    }

    public BookingResponse bookTicket(BookingRequest request) {
        String flightNumber = request.flightNumber().trim().toUpperCase();
        int seatsToBook = request.seats() == null ? 1 : request.seats();

        FlightState flight = flights.get(flightNumber);
        if (flight == null) {
            throw new FlightNotFoundException("Flight " + flightNumber + " was not found");
        }

        flight.reserveSeats(seatsToBook);

        BookingResponse booking = new BookingResponse(
                bookingIdSequence.getAndIncrement(),
                flightNumber,
                request.passengerName().trim(),
                seatsToBook
        );
        bookings.add(booking);
        logFlightAvailability("Flight availability after booking " + booking.bookingId() + ":");
        return booking;
    }

    public FlightAvailabilityResponse getFlightAvailability(String flightNumber) {
        String normalizedFlightNumber = flightNumber.trim().toUpperCase();
        FlightState flight = flights.get(normalizedFlightNumber);

        if (flight == null) {
            throw new FlightNotFoundException("Flight " + normalizedFlightNumber + " was not found");
        }

        return flight.toAvailabilityResponse();
    }

    public void cancelBooking(long bookingId) {
        BookingResponse booking = bookings.stream()
                .filter(b -> b.bookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingId + " was not found"));

        FlightState flight = flights.get(booking.flightNumber());
        if (flight != null) {
            flight.releaseSeats(booking.seats());
        }

        bookings.remove(booking);
        logFlightAvailability("Flight availability after cancellation of booking " + bookingId + ":");
    }

    public BookingResponse getBookingById(long bookingId) {
        return bookings.stream()
                .filter(b -> b.bookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingId + " was not found"));
    }

    private void logFlightAvailability(String header) {
        log.info(header);
        flights.values().stream()
                .sorted(Comparator.comparing(FlightState::flightNumber))
                .forEach(flight -> log.info("{} -> available seats: {}", flight.flightNumber(), flight.availableSeats()));
    }

    private static final class FlightState {
        private final String flightNumber;
        private final int capacity;
        private int bookedSeats;

        private FlightState(String flightNumber, int capacity) {
            this.flightNumber = flightNumber;
            this.capacity = capacity;
        }

        private synchronized void reserveSeats(int requestedSeats) {
            int remainingSeats = capacity - bookedSeats;
            if (requestedSeats > remainingSeats) {
                throw new OverbookingException(
                        "Flight " + flightNumber + " has only " + remainingSeats + " seat(s) left"
                );
            }
            bookedSeats += requestedSeats;
        }

        private synchronized void releaseSeats(int seatsToRelease) {
            bookedSeats -= seatsToRelease;
            if (bookedSeats < 0) {
                bookedSeats = 0;
            }
        }

        private String flightNumber() {
            return flightNumber;
        }

        private synchronized int availableSeats() {
            return capacity - bookedSeats;
        }

        private synchronized FlightAvailabilityResponse toAvailabilityResponse() {
            return new FlightAvailabilityResponse(
                    flightNumber,
                    capacity,
                    bookedSeats,
                    capacity - bookedSeats
            );
        }
    }
}
