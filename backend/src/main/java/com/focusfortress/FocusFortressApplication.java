package com.focusfortress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FocusFortressApplication {
    public static void main(String[] args) {
        // All server-side timestamps are UTC (matches MySQL in Docker and the frontend)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FocusFortressApplication.class, args);
    }
}
