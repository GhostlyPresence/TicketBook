package com.ticketbook.domain;

import java.time.LocalDateTime;

public record BookingAuditEntry(
        long auditId,
        long bookingId,
        String passengerName,
        String flightNumber,
        int seats,
        String action,
        String details,
        LocalDateTime timestamp
) {
}
