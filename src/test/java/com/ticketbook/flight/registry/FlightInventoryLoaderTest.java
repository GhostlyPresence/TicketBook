package com.ticketbook.flight.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlightInventoryLoaderTest {

    private final FlightInventoryLoader loader = new FlightInventoryLoader(
            new ByteArrayResource("[]".getBytes(StandardCharsets.UTF_8)),
            new ObjectMapper()
    );

    @Test
    @DisplayName("Should load flights from JSON")
    void shouldLoadFlightsFromJson() {
        List<FlightInventoryLoader.FlightInventoryEntry> inventory = loader.loadInventory(resource("""
                [
                  {"flightNumber":"TB100","capacity":5},
                  {"flightNumber":"TB200","capacity":3}
                ]
                """));

        assertEquals(2, inventory.size());
        assertEquals("TB100", inventory.get(0).flightNumber());
        assertEquals(5, inventory.get(0).capacity());
    }

    @Test
    @DisplayName("Should reject duplicate flight numbers")
    void shouldRejectDuplicateFlightNumbers() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> loader.loadInventory(resource("""
                [
                  {"flightNumber":"TB100","capacity":5},
                  {"flightNumber":"tb100","capacity":3}
                ]
                """)));

        assertEquals("Duplicate flight number in flights.json: TB100", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject invalid capacity")
    void shouldRejectInvalidCapacity() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> loader.loadInventory(resource("""
                [
                  {"flightNumber":"TB100","capacity":0}
                ]
                """)));

        assertEquals("capacity must be at least 1", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject blank flight number")
    void shouldRejectBlankFlightNumber() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> loader.loadInventory(resource("""
                [
                  {"flightNumber":" ","capacity":2}
                ]
                """)));

        assertEquals("flightNumber is required", exception.getMessage());
    }

    private static ByteArrayResource resource(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }
}