package com.ticketbook.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ticketbook.flight")
public class FlightInventoryConfig {

    @Valid
    @NotEmpty(message = "flight inventory must contain at least one flight")
    private List<FlightDefinition> inventory = new ArrayList<>();

    public List<FlightDefinition> getInventory() {
        return inventory;
    }

    public void setInventory(List<FlightDefinition> inventory) {
        this.inventory = inventory;
    }

    public static class FlightDefinition {

        @NotBlank(message = "flightNumber is required")
        private String flightNumber;

        @Min(value = 1, message = "capacity must be at least 1")
        private int capacity;

        public String getFlightNumber() {
            return flightNumber;
        }

        public void setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
}
