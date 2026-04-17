package com.ticketbook.flight.monitoring;

import com.ticketbook.flight.domain.FlightService;
import com.ticketbook.flight.registry.RegisteredFlight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Collection;

/**
 * Responsible for logging flight availability information.
 * Single Responsibility: Flight availability logging
 */
@Service
public class FlightAvailabilityLogger {

    private static final Logger log = LoggerFactory.getLogger(FlightAvailabilityLogger.class);

    private final FlightService flightService;

    public FlightAvailabilityLogger(FlightService flightService) {
        this.flightService = flightService;
    }

    public void logFlightAvailability(String header) {
        log.info(header);
        Collection<RegisteredFlight> flights = flightService.getAllFlights();
        flights.stream()
            .sorted(Comparator.comparing(RegisteredFlight::getFlightNumber))
                .forEach(flight -> log.info("{} -> available seats: {}", flight.getFlightNumber(), flight.getAvailableSeats()));
    }
}
