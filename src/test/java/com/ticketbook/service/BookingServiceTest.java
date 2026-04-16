package com.ticketbook.service;

import com.ticketbook.domain.BookingRequest;
import com.ticketbook.domain.BookingResponse;
import com.ticketbook.exception.FlightNotFoundException;
import com.ticketbook.exception.OverbookingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BookingService Tests")
class BookingServiceTest {

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService();
    }

    @Test
    @DisplayName("Should book ticket successfully with valid request")
    void testSuccessfulBooking() {
        BookingRequest request = new BookingRequest("TB100", "John Doe", 2);
        BookingResponse response = bookingService.bookTicket(request);

        assertNotNull(response);
        assertEquals("TB100", response.flightNumber());
        assertEquals("John Doe", response.passengerName());
        assertEquals(2, response.seats());
        assertTrue(response.bookingId() > 0);
    }

    @Test
    @DisplayName("Should throw FlightNotFoundException for unknown flight")
    void testUnknownFlight() {
        BookingRequest request = new BookingRequest("UNKNOWN", "Alice", 1);

        FlightNotFoundException exception = assertThrows(
                FlightNotFoundException.class,
                () -> bookingService.bookTicket(request)
        );

        assertTrue(exception.getMessage().contains("UNKNOWN"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should throw OverbookingException when seats exceed capacity")
    void testOverbookingPrevention() {
        BookingRequest request = new BookingRequest("TB100", "Bob", 10);

        OverbookingException exception = assertThrows(
                OverbookingException.class,
                () -> bookingService.bookTicket(request)
        );

        assertTrue(exception.getMessage().contains("TB100"));
        assertTrue(exception.getMessage().contains("seat"));
    }

    @Test
    @DisplayName("Should allow multiple bookings up to capacity")
    void testMultipleBookingsWithinCapacity() {
        BookingRequest request1 = new BookingRequest("TB200", "Passenger 1", 1);
        BookingRequest request2 = new BookingRequest("TB200", "Passenger 2", 1);
        BookingRequest request3 = new BookingRequest("TB200", "Passenger 3", 1);

        BookingResponse response1 = bookingService.bookTicket(request1);
        BookingResponse response2 = bookingService.bookTicket(request2);
        BookingResponse response3 = bookingService.bookTicket(request3);

        assertEquals(1, response1.bookingId());
        assertEquals(2, response2.bookingId());
        assertEquals(3, response3.bookingId());
        assertEquals("TB200", response1.flightNumber());
        assertEquals("TB200", response2.flightNumber());
        assertEquals("TB200", response3.flightNumber());
    }

    @Test
    @DisplayName("Should reject booking when capacity is exactly exhausted")
    void testExactCapacityExhaustion() {
        BookingRequest request1 = new BookingRequest("TB200", "P1", 2);
        BookingRequest request2 = new BookingRequest("TB200", "P2", 1);
        BookingRequest request3 = new BookingRequest("TB200", "P3", 1);

        bookingService.bookTicket(request1);
        bookingService.bookTicket(request2);

        OverbookingException exception = assertThrows(
                OverbookingException.class,
                () -> bookingService.bookTicket(request3)
        );

        assertTrue(exception.getMessage().contains("0 seat"));
    }

    @Test
    @DisplayName("Should handle concurrent bookings without race conditions")
    void testConcurrentBookingNoRaceCondition() throws InterruptedException {
        int threadCount = 10;
        int seatsPerThread = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // TB300 has capacity of 2, so only 2 threads should succeed
        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            executor.submit(() -> {
                try {
                    BookingRequest request = new BookingRequest(
                            "TB300",
                            "Passenger " + threadId,
                            seatsPerThread
                    );
                    bookingService.bookTicket(request);
                    successCount.incrementAndGet();
                } catch (OverbookingException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(2, successCount.get(), "Only 2 bookings should succeed for TB300 with capacity 2");
        assertEquals(8, failureCount.get(), "8 bookings should fail due to overbooking");
    }

    @Test
    @DisplayName("Should normalize flight numbers to uppercase")
    void testFlightNumberNormalization() {
        BookingRequest request = new BookingRequest("tb100", "Test", 1);
        BookingResponse response = bookingService.bookTicket(request);

        assertEquals("TB100", response.flightNumber());
    }

    @Test
    @DisplayName("Should trim passenger name and flight number whitespace")
    void testTrimmingWhitespace() {
        BookingRequest request = new BookingRequest("  TB100  ", "  John Doe  ", 1);
        BookingResponse response = bookingService.bookTicket(request);

        assertEquals("TB100", response.flightNumber());
        assertEquals("John Doe", response.passengerName());
    }

    @Test
    @DisplayName("Should assign unique booking IDs sequentially")
    void testUniqueBookingIds() {
        BookingRequest request1 = new BookingRequest("TB100", "P1", 1);
        BookingRequest request2 = new BookingRequest("TB200", "P2", 1);
        BookingRequest request3 = new BookingRequest("TB300", "P3", 1);

        BookingResponse response1 = bookingService.bookTicket(request1);
        BookingResponse response2 = bookingService.bookTicket(request2);
        BookingResponse response3 = bookingService.bookTicket(request3);

        assertEquals(1, response1.bookingId());
        assertEquals(2, response2.bookingId());
        assertEquals(3, response3.bookingId());
    }

    @Test
    @DisplayName("Should default seats to 1 if not provided")
    void testDefaultSeats() {
        BookingRequest request = new BookingRequest("TB100", "Passenger", null);
        BookingResponse response = bookingService.bookTicket(request);

        assertEquals(1, response.seats());
    }
}
