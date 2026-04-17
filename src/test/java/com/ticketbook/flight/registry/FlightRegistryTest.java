package com.ticketbook.flight.registry;

import com.ticketbook.config.FlightInventoryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlightRegistryTest {

    @Test
    @DisplayName("Should hot-reload flight inventory while preserving booked seats")
    void shouldReloadInventorySafely() {
        FlightInventoryConfig initialConfig = new FlightInventoryConfig();
        initialConfig.setInventory(List.of(flight("TB100", 5), flight("TB200", 3)));

        FlightRegistry registry = new FlightRegistry(initialConfig);
        registry.initialize();

        RegisteredFlight originalFlight = registry.findFlight("TB100");
        originalFlight.reserveSeats(2);

        registry.reload(List.of(flight("TB100", 8), flight("TB300", 4)));

        RegisteredFlight reloadedFlight = registry.findFlight("TB100");
        RegisteredFlight newFlight = registry.findFlight("TB300");

        assertNotNull(reloadedFlight);
        assertEquals(8, reloadedFlight.getCapacity());
        assertEquals(2, reloadedFlight.getBookedSeats());
        assertNotNull(newFlight);
        assertEquals(4, newFlight.getCapacity());
        assertNull(registry.findFlight("TB200"));
    }

    private static FlightInventoryConfig.FlightDefinition flight(String flightNumber, int capacity) {
        FlightInventoryConfig.FlightDefinition definition = new FlightInventoryConfig.FlightDefinition();
        definition.setFlightNumber(flightNumber);
        definition.setCapacity(capacity);
        return definition;
    }
}
