package com.ticketbook.booking.application;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.domain.FlightAvailabilityResponse;
import com.ticketbook.exception.BookingNotFoundException;
import com.ticketbook.exception.FlightNotFoundException;
import com.ticketbook.exception.OverbookingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("BookingManagementService Integration Tests")
class BookingManagementServiceTest {

    @Autowired
    private BookingManagementService bookingManagementService;

    @Test
    @DisplayName("Should book ticket successfully with valid request")
    void shouldBookTicketSuccessfully() {
        BookingRequest request = new BookingRequest("TB100", "John Doe", 1);

        BookingResponse response = bookingManagementService.bookTicket(request);

        assertNotNull(response);
        assertEquals("TB100", response.flightNumber());
        assertEquals("John Doe", response.passengerName());
        assertEquals(1, response.seats());
        assertTrue(response.bookingId() > 0);
    }

    @Test
    @DisplayName("Should throw FlightNotFoundException for unknown flight")
    void shouldThrowForUnknownFlight() {
        BookingRequest request = new BookingRequest("UNKNOWN", "Alice", 1);

        assertThrows(FlightNotFoundException.class, () -> bookingManagementService.bookTicket(request));
    }

    @Test
    @DisplayName("Should throw OverbookingException when seats exceed capacity")
    void shouldPreventOverbooking() {
        BookingRequest request = new BookingRequest("TB300", "Bob", 3);

        assertThrows(OverbookingException.class, () -> bookingManagementService.bookTicket(request));
    }

    @Test
    @DisplayName("Should free seats after cancellation")
    void shouldFreeSeatsAfterCancellation() {
        BookingRequest request = new BookingRequest("TB200", "Cancel Me", 1);
        BookingResponse booking = bookingManagementService.bookTicket(request);

        FlightAvailabilityResponse beforeCancel = bookingManagementService.getFlightAvailability("TB200");

        bookingManagementService.cancelBooking(booking.bookingId());

        FlightAvailabilityResponse afterCancel = bookingManagementService.getFlightAvailability("TB200");

        assertTrue(afterCancel.availableSeats() > beforeCancel.availableSeats());
        assertThrows(BookingNotFoundException.class,
                () -> bookingManagementService.cancelBooking(booking.bookingId()));
    }

    @Test
    @DisplayName("Should default seats to 1 when seats are not provided")
    void shouldDefaultSeatsToOne() {
        BookingRequest request = new BookingRequest("TB100", "Default Seats", null);

        BookingResponse response = bookingManagementService.bookTicket(request);

        assertEquals(1, response.seats());
    }
}
