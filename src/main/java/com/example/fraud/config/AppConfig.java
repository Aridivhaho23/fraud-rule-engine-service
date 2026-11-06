package com.example.fraud.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure beans.
 *
 * <p>A {@link Clock} is injected instead of calling {@code LocalDateTime.now()} everywhere, so tests can
 * freeze time. The whole application works in UTC.
 */
@Configuration
@EnableConfigurationProperties({FraudRulesProperties.class, FraudSecurityProperties.class})
public class AppConfig {

    @Bean
    public Clock clock() {
        // The whole application works in UTC. Tests can inject a fixed clock to freeze time.
        return Clock.systemUTC();
    }
}
