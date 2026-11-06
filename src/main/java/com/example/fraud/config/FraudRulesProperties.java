package com.example.fraud.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe configuration for every fraud rule (prefix {@code fraud.rules} in the properties files).
 *
 * <p>Thresholds live in configuration, not in code, so operations can tune the rules per environment
 * without a code change or redeploy of different code. The {@code @DefaultValue} annotations are the
 * fallback when a property is not set. Amounts are ZAR.
 */
@ConfigurationProperties(prefix = "fraud.rules")
public record FraudRulesProperties(
        @DefaultValue HighAmount highAmount,
        @DefaultValue Velocity velocity,
        @DefaultValue Geography geography,
        @DefaultValue Night night,
        @DefaultValue Gambling gambling,
        @DefaultValue CardTesting cardTesting,
        @DefaultValue Blacklist blacklist) {

    /** Flag single large transactions. */
    public record HighAmount(@DefaultValue("true") boolean enabled,
                             @DefaultValue("20000") BigDecimal threshold,
                             @DefaultValue("50000") BigDecimal criticalThreshold) {
    }

    /** Flag too many transactions in a short window. */
    public record Velocity(@DefaultValue("true") boolean enabled,
                           @DefaultValue("10m") Duration window,
                           @DefaultValue("5") int maxTransactions) {
    }

    /** Flag purchases in two different countries too close together in time ("impossible travel"). */
    public record Geography(@DefaultValue("true") boolean enabled,
                            @DefaultValue("60m") Duration window) {
    }

    /** Flag meaningful purchases in the middle of the night (local time of {@code zone}). */
    public record Night(@DefaultValue("true") boolean enabled,
                        @DefaultValue("1") int startHour,
                        @DefaultValue("5") int endHour,
                        @DefaultValue("500") BigDecimal minAmount,
                        @DefaultValue("Africa/Johannesburg") String zone) {
    }

    /** Flag large or repeated gambling spend. */
    public record Gambling(@DefaultValue("true") boolean enabled,
                           @DefaultValue("5000") BigDecimal largeAmount,
                           @DefaultValue("24h") Duration repeatWindow,
                           @DefaultValue("3") int repeatCount) {
    }

    /** Flag bursts of tiny transactions (criminals "test" a stolen card with small amounts). */
    public record CardTesting(@DefaultValue("true") boolean enabled,
                              @DefaultValue("20") BigDecimal smallAmount,
                              @DefaultValue("10m") Duration window,
                              @DefaultValue("3") int minAttempts) {
    }

    /** Flag any purchase at a blacklisted merchant. */
    public record Blacklist(@DefaultValue("true") boolean enabled) {
    }
}
