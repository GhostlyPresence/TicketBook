package com.ticketbook.booking.domain;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.exception.BookingNotFoundException;
import com.ticketbook.exception.BookingPolicyViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsible for managing booking records.
 * Single Responsibility: Booking lifecycle and persistence
 */
@Service
public class BookingService {

    private final AtomicLong bookingIdSequence = new AtomicLong(1);
    private final Map<Long, BookingRecord> bookings = new ConcurrentHashMap<>();

    public BookingResponse createBooking(BookingRequest request, int seatsToBook) {
        BookingResponse booking = new BookingResponse(
                bookingIdSequence.getAndIncrement(),
                request.flightNumber().trim().toUpperCase(),
                request.passengerName().trim(),
                seatsToBook
        );
        bookings.put(booking.bookingId(), new BookingRecord(booking));
        return booking;
    }

    public BookingResponse getBookingById(long bookingId) {
        return getBookingRecord(bookingId).booking();
    }

    public void registerCancellation(long bookingId, int maxCancellationsPerBooking) {
        BookingRecord bookingRecord = getBookingRecord(bookingId);

        synchronized (bookingRecord) {
            if (bookingRecord.cancellationCount() >= maxCancellationsPerBooking) {
                throw new BookingPolicyViolationException(
                        "Booking " + bookingId + " has reached the cancellation limit"
                );
            }

            bookingRecord.incrementCancellationCount();
        }
    }

    public void deleteBooking(long bookingId) {
        getBookingRecord(bookingId);
        bookings.remove(bookingId);
    }

    public List<BookingResponse> getAllBookings() {
        return bookings.values().stream()
                .map(BookingRecord::booking)
                .toList();
    }

    private BookingRecord getBookingRecord(long bookingId) {
        BookingRecord bookingRecord = bookings.get(bookingId);
        if (bookingRecord == null) {
            throw new BookingNotFoundException("Booking " + bookingId + " was not found");
        }
        return bookingRecord;
    }

    private static final class BookingRecord {
        private final BookingResponse booking;
        private int cancellationCount;

        private BookingRecord(BookingResponse booking) {
            this.booking = booking;
        }

        private BookingResponse booking() {
            return booking;
        }

        private int cancellationCount() {
            return cancellationCount;
        }

        private void incrementCancellationCount() {
            cancellationCount++;
        }
    }
}
