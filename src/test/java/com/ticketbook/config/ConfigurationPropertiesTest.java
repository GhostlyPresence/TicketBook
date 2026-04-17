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
            .withUserConfiguration(TestPropertiesConfiguration.class);

    @Test
    @DisplayName("Should bind booking configuration properties")
    void shouldBindConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "ticketbook.booking.max-seats-per-passenger=4",
                        "ticketbook.booking.max-cancellations-per-booking=2"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    BookingConfig bookingConfig = context.getBean(BookingConfig.class);

                    assertThat(bookingConfig.getMaxSeatsPerPassenger()).isEqualTo(4);
                    assertThat(bookingConfig.getMaxCancellationsPerBooking()).isEqualTo(2);
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

    @Configuration(proxyBeanMethods = false)
        @EnableConfigurationProperties(BookingConfig.class)
    static class TestPropertiesConfiguration {
    }
}
