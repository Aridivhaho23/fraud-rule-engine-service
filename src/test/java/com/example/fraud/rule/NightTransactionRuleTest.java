package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Stored times are UTC; South Africa (Africa/Johannesburg) is UTC+2 all year.
 * So 01:30 UTC = 03:30 local = night, while 10:00 UTC = 12:00 local = day.
 */
class NightTransactionRuleTest {

    private final NightTransactionRule rule = new NightTransactionRule(TestData.defaultRules());

    @Test
    void bigPurchaseAtLocalNight_isMedium() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 20, 1, 30);

        RuleResult result = rule.evaluate(TestData.tx("1500.00", MerchantCategory.RETAIL, "Shop", "ZA", utc)).orElseThrow();

        assertEquals(Severity.MEDIUM, result.severity());
    }

    @Test
    void purchaseDuringTheDay_isNotFlagged() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 20, 10, 0);

        assertTrue(rule.evaluate(TestData.tx("1500.00", MerchantCategory.RETAIL, "Shop", "ZA", utc)).isEmpty());
    }

    @Test
    void smallPurchaseAtNight_isNotFlagged() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 20, 1, 30);

        assertTrue(rule.evaluate(TestData.tx("50.00", MerchantCategory.FUEL, "Garage", "ZA", utc)).isEmpty());
    }

    @Test
    void boundary_localFiveOClockIsOutsideTheWindow() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 20, 3, 0); // 05:00 local

        assertTrue(rule.evaluate(TestData.tx("1500.00", MerchantCategory.RETAIL, "Shop", "ZA", utc)).isEmpty());
    }
}
