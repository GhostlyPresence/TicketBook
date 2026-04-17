package com.ticketbook.flight.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class FlightInventoryLoader {

    private static final TypeReference<List<FlightInventoryEntry>> FLIGHT_LIST_TYPE = new TypeReference<>() {
    };

    private final Resource flightsResource;
    private final ObjectMapper objectMapper;

    public FlightInventoryLoader(
            @Value("classpath:flights.json") Resource flightsResource,
            ObjectMapper objectMapper) {
        this.flightsResource = flightsResource;
        this.objectMapper = objectMapper;
    }

    public List<FlightInventoryEntry> loadInventory() {
        return loadInventory(flightsResource);
    }

    List<FlightInventoryEntry> loadInventory(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            List<FlightInventoryEntry> inventory = objectMapper.readValue(inputStream, FLIGHT_LIST_TYPE);
            validate(inventory);
            return List.copyOf(inventory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load flights.json", exception);
        }
    }

    private void validate(List<FlightInventoryEntry> inventory) {
        if (inventory == null || inventory.isEmpty()) {
            throw new IllegalStateException("flights.json must contain at least one flight");
        }

        Set<String> flightNumbers = new HashSet<>();
        for (FlightInventoryEntry entry : inventory) {
            if (entry.flightNumber() == null || entry.flightNumber().isBlank()) {
                throw new IllegalStateException("flightNumber is required");
            }

            if (entry.capacity() < 1) {
                throw new IllegalStateException("capacity must be at least 1");
            }

            String normalizedFlightNumber = entry.flightNumber().trim().toUpperCase(Locale.ROOT);
            if (!flightNumbers.add(normalizedFlightNumber)) {
                throw new IllegalStateException("Duplicate flight number in flights.json: " + normalizedFlightNumber);
            }
        }
    }

    public record FlightInventoryEntry(String flightNumber, int capacity) {
    }
}
