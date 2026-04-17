package com.ticketbook;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TicketBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketBookApplication.class, args);
    }
}
