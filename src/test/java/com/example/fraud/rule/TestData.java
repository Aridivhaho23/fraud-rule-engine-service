package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Customer;
import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Transaction;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Helpers that build entities and rule settings for unit tests, so each test reads like a sentence.
 * Ids are normally assigned by the database; in unit tests we set them with ReflectionTestUtils.
 */
public final class TestData {

    private TestData() {
    }

    /** The settings used in production by default (same numbers as application.properties). */
    public static FraudRulesProperties defaultRules() {
        return new FraudRulesProperties(
                new FraudRulesProperties.HighAmount(true, new BigDecimal("20000"), new BigDecimal("50000")),
                new FraudRulesProperties.Velocity(true, Duration.ofMinutes(10), 5),
                new FraudRulesProperties.Geography(true, Duration.ofMinutes(60)),
                new FraudRulesProperties.Night(true, 1, 5, new BigDecimal("500"), "Africa/Johannesburg"),
                new FraudRulesProperties.Gambling(true, new BigDecimal("5000"), Duration.ofHours(24), 3),
                new FraudRulesProperties.CardTesting(true, new BigDecimal("20"), Duration.ofMinutes(10), 3),
                new FraudRulesProperties.Blacklist(true));
    }

    public static Customer customer(long id) {
        Customer customer = new Customer("Test Customer", "customer" + id + "@example.com", "ZA",
                LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    public static Transaction tx(String amount, MerchantCategory category, String merchant, String country,
                                 LocalDateTime occurredAt) {
        Transaction tx = new Transaction(customer(1L), "REF-1", new BigDecimal(amount), merchant, category,
                country, occurredAt, occurredAt);
        ReflectionTestUtils.setField(tx, "id", 100L);
        return tx;
    }

    /** A plain R100 retail purchase in South Africa at midday UTC. */
    public static Transaction ordinaryTx() {
        return tx("100.00", MerchantCategory.RETAIL, "Corner Shop", "ZA", LocalDateTime.of(2026, 10, 20, 10, 0));
    }
}
