package com.ticketbook.flight.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlightRegistryTest {

    @Test
    @DisplayName("Should hot-reload flight inventory while preserving booked seats")
    void shouldReloadInventorySafely() {
        FlightInventoryLoader loader = new FlightInventoryLoader(
            new ByteArrayResource("[]".getBytes(StandardCharsets.UTF_8)),
            new ObjectMapper()
        );

        FlightRegistry registry = new FlightRegistry(loader);
        registry.reload(List.of(
            new FlightInventoryLoader.FlightInventoryEntry("TB100", 5),
            new FlightInventoryLoader.FlightInventoryEntry("TB200", 3)
        ));

        RegisteredFlight originalFlight = registry.findFlight("TB100");
        originalFlight.reserveSeats(2);

        registry.reload(List.of(
            new FlightInventoryLoader.FlightInventoryEntry("TB100", 8),
            new FlightInventoryLoader.FlightInventoryEntry("TB300", 4)
        ));

        RegisteredFlight reloadedFlight = registry.findFlight("TB100");
        RegisteredFlight newFlight = registry.findFlight("TB300");

        assertNotNull(reloadedFlight);
        assertEquals(8, reloadedFlight.getCapacity());
        assertEquals(2, reloadedFlight.getBookedSeats());
        assertNotNull(newFlight);
        assertEquals(4, newFlight.getCapacity());
        assertNull(registry.findFlight("TB200"));
    }
}
