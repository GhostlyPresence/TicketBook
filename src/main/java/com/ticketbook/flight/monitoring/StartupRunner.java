package com.ticketbook.flight.monitoring;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs on application startup to log initial flight availability.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private final FlightAvailabilityLogger availabilityLogger;

    public StartupRunner(FlightAvailabilityLogger availabilityLogger) {
        this.availabilityLogger = availabilityLogger;
    }

    @Override
    public void run(ApplicationArguments args) {
        availabilityLogger.logFlightAvailability("Flight availability at startup:");
    }
}
