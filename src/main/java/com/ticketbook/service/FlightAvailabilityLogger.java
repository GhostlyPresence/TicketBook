package com.ticketbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;

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
        Map<String, FlightService.Flight> flights = flightService.getAllFlights();
        flights.values().stream()
                .sorted(Comparator.comparing(FlightService.Flight::getFlightNumber))
                .forEach(flight -> log.info("{} -> available seats: {}", flight.getFlightNumber(), flight.getAvailableSeats()));
    }
}
