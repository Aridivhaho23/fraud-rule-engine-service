package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** One unit test class per rule: fast, no Spring, no database. */
class HighAmountRuleTest {

    private final HighAmountRule rule = new HighAmountRule(TestData.defaultRules());
    private final LocalDateTime when = LocalDateTime.of(2026, 10, 20, 10, 0);

    @Test
    void ordinaryAmount_isNotFlagged() {
        assertTrue(rule.evaluate(TestData.tx("19999.99", MerchantCategory.RETAIL, "Shop", "ZA", when)).isEmpty());
    }

    @Test
    void amountAtThreshold_isHigh() {
        Optional<RuleResult> result = rule.evaluate(TestData.tx("20000.00", MerchantCategory.RETAIL, "Shop", "ZA", when));

        assertEquals(Severity.HIGH, result.orElseThrow().severity());
        assertEquals(HighAmountRule.CODE, result.get().ruleCode());
    }

    @Test
    void amountAtCriticalThreshold_isCritical() {
        Optional<RuleResult> result = rule.evaluate(TestData.tx("50000.00", MerchantCategory.RETAIL, "Shop", "ZA", when));

        assertEquals(Severity.CRITICAL, result.orElseThrow().severity());
    }
}
