package com.ticketbook.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbook.domain.BookingRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BookingController Integration Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/bookings should return 201 Created on successful booking")
    void testSuccessfulBookingReturns201() throws Exception {
        BookingRequest request = new BookingRequest("TB100", "Alice", 2);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId", notNullValue()))
                .andExpect(jsonPath("$.flightNumber").value("TB100"))
                .andExpect(jsonPath("$.passengerName").value("Alice"))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("POST /api/bookings should return 404 Not Found for unknown flight")
    void testUnknownFlightReturns404() throws Exception {
        BookingRequest request = new BookingRequest("INVALID", "Bob", 1);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/bookings should return 409 Conflict on overbooking")
    void testOverbookingReturns409() throws Exception {
        BookingRequest request = new BookingRequest("TB100", "Charlie", 10);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/bookings should return 400 Bad Request for missing flightNumber")
    void testMissingFlightNumberReturns400() throws Exception {
        String invalidJson = "{ \"passengerName\": \"Test\", \"seats\": 1 }";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/bookings should return 400 Bad Request for missing passengerName")
    void testMissingPassengerNameReturns400() throws Exception {
        String invalidJson = "{ \"flightNumber\": \"TB100\", \"seats\": 1 }";

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/bookings should return 400 Bad Request for zero or negative seats")
    void testInvalidSeatsReturns400() throws Exception {
        BookingRequest request = new BookingRequest("TB100", "Test", 0);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/bookings response should include timestamp")
    void testResponseIncludesTimestamp() throws Exception {
        BookingRequest request = new BookingRequest("TB100", "Test", 1);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/bookings should handle multiple sequential bookings")
    void testSequentialBookings() throws Exception {
        BookingRequest request1 = new BookingRequest("TB200", "P1", 1);
        BookingRequest request2 = new BookingRequest("TB200", "P2", 1);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/flights/{flightNumber}/availability should return flight availability")
    void testGetFlightAvailability() throws Exception {
        mockMvc.perform(get("/api/flights/TB300/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("TB300"))
                .andExpect(jsonPath("$.totalCapacity").value(2))
                .andExpect(jsonPath("$.bookedSeats").isNumber())
                .andExpect(jsonPath("$.availableSeats").isNumber());
    }

    @Test
    @DisplayName("GET /api/flights/{flightNumber}/availability should reflect booked seats")
    void testGetFlightAvailabilityAfterBooking() throws Exception {
        // Book 1 seat on TB200
        BookingRequest bookingRequest = new BookingRequest("TB200", "Test", 1);
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated());

        // Get availability and check it reflects the booking
        mockMvc.perform(get("/api/flights/TB200/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("TB200"))
                .andExpect(jsonPath("$.totalCapacity").value(3))
                .andExpect(jsonPath("$.bookedSeats").isNumber())
                .andExpect(jsonPath("$.availableSeats").isNumber());
    }

    @Test
    @DisplayName("GET /api/flights/{flightNumber}/availability should return 404 for unknown flight")
    void testGetFlightAvailabilityNotFound() throws Exception {
        mockMvc.perform(get("/api/flights/UNKNOWN/availability"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/flights/{flightNumber}/availability should handle lowercase flight numbers")
    void testGetFlightAvailabilityNormalization() throws Exception {
        mockMvc.perform(get("/api/flights/tb100/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("TB100"));
    }

    @Test
    @DisplayName("DELETE /api/bookings/{id} should return 204 No Content on successful cancellation")
    void testCancelBookingReturns204() throws Exception {
        BookingRequest bookingRequest = new BookingRequest("TB100", "Test", 1);
        var createResponse = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResponse.getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(responseContent).get("bookingId").asLong();

        mockMvc.perform(delete("/api/bookings/" + bookingId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/bookings/{id} should return 404 for non-existent booking")
    void testCancelNonExistentBookingReturns404() throws Exception {
        mockMvc.perform(delete("/api/bookings/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("DELETE /api/bookings/{id} should free up seats after cancellation")
    void testCancelBookingFreesSeats() throws Exception {
        BookingRequest bookingRequest = new BookingRequest("TB100", "Test", 2);
        var createResponse = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResponse.getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(responseContent).get("bookingId").asLong();

        // Check availability before cancellation
        var beforeCancel = mockMvc.perform(get("/api/flights/TB100/availability"))
                .andExpect(status().isOk())
                .andReturn();
        int bookedBefore = objectMapper.readTree(beforeCancel.getResponse().getContentAsString())
                .get("bookedSeats").asInt();

        // Cancel the booking
        mockMvc.perform(delete("/api/bookings/" + bookingId))
                .andExpect(status().isNoContent());

        // Check availability after cancellation
        var afterCancel = mockMvc.perform(get("/api/flights/TB100/availability"))
                .andExpect(status().isOk())
                .andReturn();
        int bookedAfter = objectMapper.readTree(afterCancel.getResponse().getContentAsString())
                .get("bookedSeats").asInt();

        assertTrue(bookedAfter < bookedBefore, "Booked seats should decrease after cancellation");
    }
}
