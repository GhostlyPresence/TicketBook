package com.ticketbook.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ticketbook.booking")
public class BookingConfig {

    @Min(value = 1, message = "maxSeatsPerPassenger must be at least 1")
    private int maxSeatsPerPassenger;

    @Min(value = 0, message = "maxCancellationsPerBooking must be zero or greater")
    private int maxCancellationsPerBooking;

    public int getMaxSeatsPerPassenger() {
        return maxSeatsPerPassenger;
    }

    public void setMaxSeatsPerPassenger(int maxSeatsPerPassenger) {
        this.maxSeatsPerPassenger = maxSeatsPerPassenger;
    }

    public int getMaxCancellationsPerBooking() {
        return maxCancellationsPerBooking;
    }

    public void setMaxCancellationsPerBooking(int maxCancellationsPerBooking) {
        this.maxCancellationsPerBooking = maxCancellationsPerBooking;
    }
}
