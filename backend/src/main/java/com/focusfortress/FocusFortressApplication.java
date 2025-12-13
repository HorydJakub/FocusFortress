package com.focusfortress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FocusFortressApplication {
    public static void main(String[] args) {
        SpringApplication.run(FocusFortressApplication.class, args);
    }
}
