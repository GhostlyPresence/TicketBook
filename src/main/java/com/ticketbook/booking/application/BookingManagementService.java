package com.ticketbook.booking.application;

import com.ticketbook.domain.BookingAuditEntry;
import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.booking.audit.BookingAuditLog;
import com.ticketbook.booking.domain.BookingService;
import com.ticketbook.flight.application.SeatReservationService;
import com.ticketbook.flight.domain.FlightService;
import com.ticketbook.flight.monitoring.FlightAvailabilityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade that orchestrates the booking workflow by delegating to specialized services.
 * Single Responsibility: Booking orchestration and workflow coordination
 */
@Service
public class BookingManagementService {

    private static final Logger log = LoggerFactory.getLogger(BookingManagementService.class);

    private final BookingService bookingService;
    private final FlightService flightService;
    private final SeatReservationService seatReservationService;
    private final BookingAuditLog auditLog;
    private final FlightAvailabilityLogger availabilityLogger;

    public BookingManagementService(
            BookingService bookingService,
            FlightService flightService,
            SeatReservationService seatReservationService,
            BookingAuditLog auditLog,
            FlightAvailabilityLogger availabilityLogger) {
        this.bookingService = bookingService;
        this.flightService = flightService;
        this.seatReservationService = seatReservationService;
        this.auditLog = auditLog;
        this.availabilityLogger = availabilityLogger;
    }

    public BookingResponse bookTicket(BookingRequest request) {
        log.info("Processing booking request for passenger: {} on flight: {}", request.passengerName(), request.flightNumber());
        
        String flightNumber = request.flightNumber().trim().toUpperCase();
        int seatsToBook = request.seats() == null ? 1 : request.seats();
        
        // Validate flight exists
        flightService.getFlightByNumber(flightNumber);
        
        // Reserve seats
        seatReservationService.reserveSeats(flightNumber, seatsToBook);
        
        // Create booking record
        BookingResponse booking = bookingService.createBooking(request, seatsToBook);
        
        // Log to audit
        auditLog.logBooking(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats()
        );
        
        availabilityLogger.logFlightAvailability("Flight availability after booking " + booking.bookingId() + ":");
        log.info("Booking created with ID: {} and audit logged", booking.bookingId());
        return booking;
    }

    public FlightAvailabilityResponse getFlightAvailability(String flightNumber) {
        log.debug("Fetching availability for flight: {}", flightNumber);
        return flightService.getFlightAvailability(flightNumber);
    }

    public void cancelBooking(long bookingId) {
        log.info("Processing cancellation for booking ID: {}", bookingId);
        
        // Fetch booking details
        BookingResponse booking = bookingService.getBookingById(bookingId);
        
        // Release seats
        seatReservationService.releaseSeats(booking.flightNumber(), booking.seats());
        
        // Delete booking record
        bookingService.deleteBooking(bookingId);
        
        // Log the cancellation
        auditLog.logCancellation(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats()
        );
        
        availabilityLogger.logFlightAvailability("Flight availability after cancellation of booking " + bookingId + ":");
        log.info("Booking ID: {} cancelled and cancellation audit logged", bookingId);
    }

    public void refundBooking(long bookingId, String reason) {
        log.info("Processing refund for booking ID: {} with reason: {}", bookingId, reason);
        
        // Fetch booking details
        BookingResponse booking = bookingService.getBookingById(bookingId);
        
        // Release seats
        seatReservationService.releaseSeats(booking.flightNumber(), booking.seats());
        
        // Delete booking record
        bookingService.deleteBooking(bookingId);
        
        // Log the refund
        auditLog.logRefund(
                booking.bookingId(),
                booking.passengerName(),
                booking.flightNumber(),
                booking.seats(),
                reason
        );
        
        availabilityLogger.logFlightAvailability("Flight availability after refund of booking " + bookingId + ":");
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
}
