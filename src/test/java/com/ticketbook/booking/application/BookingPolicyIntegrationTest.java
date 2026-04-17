package com.ticketbook.booking.application;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.exception.BookingPolicyViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "ticketbook.booking.max-seats-per-passenger=2",
        "ticketbook.booking.max-cancellations-per-booking=0"
})
@DisplayName("Booking policy limit enforcement tests")
class BookingPolicyIntegrationTest {

    @Autowired
    private BookingManagementService bookingManagementService;

    @Test
    @DisplayName("Should reject bookings above configured seat limit")
    void shouldRejectBookingAboveConfiguredSeatLimit() {
        BookingRequest request = new BookingRequest("TB100", "Policy Test", 3);

        assertThrows(BookingPolicyViolationException.class, () -> bookingManagementService.bookTicket(request));
    }

    @Test
    @DisplayName("Should reject cancellation when configured cancellation limit is zero")
    void shouldRejectCancellationWhenLimitReached() {
        BookingResponse booking = bookingManagementService.bookTicket(new BookingRequest("TB100", "Cancel Test", 1));

        assertThrows(BookingPolicyViolationException.class,
                () -> bookingManagementService.cancelBooking(booking.bookingId()));
    }
}
