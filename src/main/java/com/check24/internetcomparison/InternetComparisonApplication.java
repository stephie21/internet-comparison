package com.check24.internetcomparison;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InternetComparisonApplication {
    public static void main(String[] args) {
        SpringApplication.run(InternetComparisonApplication.class, args);
    }
}
