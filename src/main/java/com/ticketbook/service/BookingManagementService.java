package com.ticketbook.service;

import com.ticketbook.domain.BookingAuditEntry;
import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.domain.FlightAvailabilityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingManagementService {

    private static final Logger log = LoggerFactory.getLogger(BookingManagementService.class);

    private final BookingService bookingService;
    private final BookingAuditLog auditLog;

    public BookingManagementService(BookingService bookingService, BookingAuditLog auditLog) {
        this.bookingService = bookingService;
        this.auditLog = auditLog;
    }

    public BookingResponse bookTicket(BookingRequest request) {
        log.info("Processing booking request for passenger: {} on flight: {}", request.passengerName(), request.flightNumber());
        
        BookingResponse booking = bookingService.bookTicket(request);
        
        auditLog.logBooking(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats()
        );
        
        log.info("Booking created with ID: {} and audit logged", booking.bookingId());
        return booking;
    }

    public FlightAvailabilityResponse getFlightAvailability(String flightNumber) {
        log.debug("Fetching availability for flight: {}", flightNumber);
        return bookingService.getFlightAvailability(flightNumber);
    }

    public void cancelBooking(long bookingId) {
        log.info("Processing cancellation for booking ID: {}", bookingId);
        
        // Fetch booking details before cancellation to log them
        BookingResponse booking = getBookingDetails(bookingId);
        
        // Cancel the booking
        bookingService.cancelBooking(bookingId);
        
        // Log the cancellation
        auditLog.logCancellation(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats()
        );
        
        log.info("Booking ID: {} cancelled and cancellation audit logged", bookingId);
    }

    public void refundBooking(long bookingId, String reason) {
        log.info("Processing refund for booking ID: {} with reason: {}", bookingId, reason);
        
        // Fetch booking details before cancellation
        BookingResponse booking = getBookingDetails(bookingId);
        
        // Cancel the booking (free up seats)
        bookingService.cancelBooking(bookingId);
        
        // Log the refund
        auditLog.logRefund(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats(),
                reason
        );
        
        log.info("Booking ID: {} refunded and refund audit logged", bookingId);
    }

    public List<BookingAuditEntry> getAuditLog() {
        return auditLog.getAuditLog();
    }

    public List<BookingAuditEntry> getAuditLogForBooking(long bookingId) {
        return auditLog.getAuditLogForBooking(bookingId);
    }

    public List<BookingAuditEntry> getBookingAuditLog() {
        return auditLog.getAuditLogByAction("BOOKING");
    }

    public List<BookingAuditEntry> getCancellationAuditLog() {
        return auditLog.getAuditLogByAction("CANCELLATION");
    }

    public List<BookingAuditEntry> getRefundAuditLog() {
        return auditLog.getAuditLogByAction("REFUND");
    }

    private BookingResponse getBookingDetails(long bookingId) {
        return bookingService.getBookingById(bookingId);
    }
}
