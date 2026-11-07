package com.halcyon.uniplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UniPlaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniPlaceApplication.class, args);
    }
}
