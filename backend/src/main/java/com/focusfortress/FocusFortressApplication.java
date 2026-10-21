package com.focusfortress;

import org.springframework.boot.SpringApplication;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FocusFortressApplication {
    public static void main(String[] args) {
        // All server-side timestamps are UTC (matches MySQL in Docker and the frontend)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FocusFortressApplication.class, args);
    }
}
