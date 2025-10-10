package com.tomabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TomaBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TomaBotApplication.class, args);
    }
}