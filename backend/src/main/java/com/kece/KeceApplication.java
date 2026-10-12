package com.kece;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KeceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeceApplication.class, args);
    }
}
