package com.ticketbook.flight.registry;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FlightRegistry {

    private final FlightInventoryLoader flightInventoryLoader;
    private final AtomicReference<Map<String, RegisteredFlight>> flightsRef =
            new AtomicReference<>(Collections.emptyMap());

    public FlightRegistry(FlightInventoryLoader flightInventoryLoader) {
        this.flightInventoryLoader = flightInventoryLoader;
    }

    @PostConstruct
    public void initialize() {
        reloadFromConfig();
    }

    public RegisteredFlight findFlight(String flightNumber) {
        return flightsRef.get().get(normalize(flightNumber));
    }

    public Collection<RegisteredFlight> getAllFlights() {
        return flightsRef.get().values();
    }

    public synchronized void reloadFromConfig() {
        reload(flightInventoryLoader.loadInventory());
    }

    public synchronized void reload(Collection<FlightInventoryLoader.FlightInventoryEntry> definitions) {
        Map<String, RegisteredFlight> currentFlights = flightsRef.get();
        Map<String, RegisteredFlight> reloadedFlights = new LinkedHashMap<>();

        for (FlightInventoryLoader.FlightInventoryEntry definition : definitions) {
            String normalizedFlightNumber = normalize(definition.flightNumber());
            if (reloadedFlights.containsKey(normalizedFlightNumber)) {
                throw new IllegalStateException("Duplicate flight number in configuration: " + normalizedFlightNumber);
            }

            RegisteredFlight existingFlight = currentFlights.get(normalizedFlightNumber);
            int bookedSeats = existingFlight == null ? 0 : existingFlight.getBookedSeats();

            reloadedFlights.put(
                    normalizedFlightNumber,
                    new RegisteredFlight(normalizedFlightNumber, definition.capacity(), bookedSeats)
            );
        }

        flightsRef.set(Collections.unmodifiableMap(reloadedFlights));
    }

    private String normalize(String flightNumber) {
        return flightNumber.trim().toUpperCase(Locale.ROOT);
    }
}
