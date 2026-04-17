package com.ticketbook.service;

import com.ticketbook.domain.BookingAuditEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BookingAuditLog {

    private final Map<Long, BookingAuditEntry> auditLog = new ConcurrentHashMap<>();
    private final AtomicLong auditIdSequence = new AtomicLong(1);

    public BookingAuditEntry logBooking(long bookingId, String passengerName, String flightNumber, int seats) {
        BookingAuditEntry entry = new BookingAuditEntry(
                auditIdSequence.getAndIncrement(),
                bookingId,
                passengerName,
                flightNumber,
                seats,
                "BOOKING",
                "Booking created successfully",
                LocalDateTime.now()
        );
        auditLog.put(entry.auditId(), entry);
        return entry;
    }

    public BookingAuditEntry logCancellation(long bookingId, String passengerName, String flightNumber, int seats) {
        BookingAuditEntry entry = new BookingAuditEntry(
                auditIdSequence.getAndIncrement(),
                bookingId,
                passengerName,
                flightNumber,
                seats,
                "CANCELLATION",
                "Booking cancelled",
                LocalDateTime.now()
        );
        auditLog.put(entry.auditId(), entry);
        return entry;
    }

    public BookingAuditEntry logRefund(long bookingId, String passengerName, String flightNumber, int seats, String reason) {
        BookingAuditEntry entry = new BookingAuditEntry(
                auditIdSequence.getAndIncrement(),
                bookingId,
                passengerName,
                flightNumber,
                seats,
                "REFUND",
                "Refund processed - " + reason,
                LocalDateTime.now()
        );
        auditLog.put(entry.auditId(), entry);
        return entry;
    }

    public List<BookingAuditEntry> getAuditLog() {
        return new ArrayList<>(auditLog.values());
    }

    public List<BookingAuditEntry> getAuditLogForBooking(long bookingId) {
        return auditLog.values().stream()
                .filter(entry -> entry.bookingId() == bookingId)
                .sorted(Comparator.comparing(BookingAuditEntry::timestamp))
                .toList();
    }

    public List<BookingAuditEntry> getAuditLogByAction(String action) {
        return auditLog.values().stream()
                .filter(entry -> entry.action().equals(action))
                .sorted(Comparator.comparing(BookingAuditEntry::timestamp).reversed())
                .toList();
    }

    public void clearAuditLog() {
        auditLog.clear();
    }
}
