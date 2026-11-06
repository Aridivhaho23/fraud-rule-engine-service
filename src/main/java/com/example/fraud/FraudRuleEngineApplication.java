package com.example.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. {@code @SpringBootApplication} turns on component scanning (finds our @Service, @Component,
 * @RestController ... classes below this package), auto-configuration (Tomcat, Hibernate, Flyway, Security ...)
 * and configuration support.
 */
@SpringBootApplication
public class FraudRuleEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudRuleEngineApplication.class, args);
    }
}
