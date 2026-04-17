package com.ticketbook.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(TestPropertiesConfiguration.class)
            .withPropertyValues(
                    "ticketbook.flight.inventory[0].flight-number=TB100",
                    "ticketbook.flight.inventory[0].capacity=5",
                    "ticketbook.flight.inventory[1].flight-number=TB200",
                    "ticketbook.flight.inventory[1].capacity=3"
            );

    @Test
    @DisplayName("Should bind booking and flight configuration properties")
    void shouldBindConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "ticketbook.booking.max-seats-per-passenger=4",
                        "ticketbook.booking.max-cancellations-per-booking=2"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    BookingConfig bookingConfig = context.getBean(BookingConfig.class);
                    FlightInventoryConfig flightInventoryConfig = context.getBean(FlightInventoryConfig.class);

                    assertThat(bookingConfig.getMaxSeatsPerPassenger()).isEqualTo(4);
                    assertThat(bookingConfig.getMaxCancellationsPerBooking()).isEqualTo(2);
                    assertThat(flightInventoryConfig.getInventory()).hasSize(2);
                    assertThat(flightInventoryConfig.getInventory().get(0).getFlightNumber()).isEqualTo("TB100");
                });
    }

    @Test
    @DisplayName("Should fail validation for invalid booking configuration")
    void shouldFailValidationForInvalidBookingConfiguration() {
        contextRunner
                .withPropertyValues(
                        "ticketbook.booking.max-seats-per-passenger=0",
                        "ticketbook.booking.max-cancellations-per-booking=-1"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                                        assertThat(context.getStartupFailure()).hasStackTraceContaining("maxSeatsPerPassenger must be at least 1");
                                        assertThat(context.getStartupFailure()).hasStackTraceContaining("maxCancellationsPerBooking must be zero or greater");
                });
    }

    @Test
    @DisplayName("Should fail validation for invalid flight inventory configuration")
    void shouldFailValidationForInvalidFlightInventoryConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        ValidationAutoConfiguration.class
                ))
                .withUserConfiguration(TestPropertiesConfiguration.class)
                .withPropertyValues(
                        "ticketbook.booking.max-seats-per-passenger=2",
                        "ticketbook.booking.max-cancellations-per-booking=1",
                        "ticketbook.flight.inventory[0].flight-number=",
                        "ticketbook.flight.inventory[0].capacity=0"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                                        assertThat(context.getStartupFailure()).hasStackTraceContaining("flightNumber is required");
                                        assertThat(context.getStartupFailure()).hasStackTraceContaining("capacity must be at least 1");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({BookingConfig.class, FlightInventoryConfig.class})
    static class TestPropertiesConfiguration {
    }
}
