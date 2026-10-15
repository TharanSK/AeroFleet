package com.aerofleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AeroFleetApplication {

    public static void main(String[] args) {
        SpringApplication.run(AeroFleetApplication.class, args);
    }
}
