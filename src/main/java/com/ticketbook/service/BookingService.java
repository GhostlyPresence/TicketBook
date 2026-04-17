package com.ticketbook.service;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.exception.BookingNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsible for managing booking records.
 * Single Responsibility: Booking lifecycle and persistence
 */
@Service
public class BookingService {

    private final AtomicLong bookingIdSequence = new AtomicLong(1);
    private final List<BookingResponse> bookings = new CopyOnWriteArrayList<>();

    public BookingResponse createBooking(BookingRequest request, int seatsToBook) {
        BookingResponse booking = new BookingResponse(
                bookingIdSequence.getAndIncrement(),
                request.flightNumber().trim().toUpperCase(),
                request.passengerName().trim(),
                seatsToBook
        );
        bookings.add(booking);
        return booking;
    }

    public BookingResponse getBookingById(long bookingId) {
        return bookings.stream()
                .filter(b -> b.bookingId() == bookingId)
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingId + " was not found"));
    }

    public void deleteBooking(long bookingId) {
        BookingResponse booking = getBookingById(bookingId);
        bookings.remove(booking);
    }

    public List<BookingResponse> getAllBookings() {
        return List.copyOf(bookings);
    }
}
