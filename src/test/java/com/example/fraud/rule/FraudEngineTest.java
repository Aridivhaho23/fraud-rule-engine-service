package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the ENGINE with tiny fake rules, proving it works for any FraudRule implementation:
 * it collects results, skips disabled rules, and never needs to know a concrete rule class.
 */
class FraudEngineTest {

    /** A hand-written test double: a rule with a fixed behaviour. */
    private record FakeRule(String code, boolean enabled, boolean fires) implements FraudRule {
        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public Optional<RuleResult> evaluate(Transaction transaction) {
            return fires ? Optional.of(new RuleResult(code, Severity.LOW, 10, "fake")) : Optional.empty();
        }
    }

    @Test
    void collectsResultsFromAllRulesThatFire() {
        FraudEngine engine = new FraudEngine(List.of(
                new FakeRule("A", true, true), new FakeRule("B", true, false), new FakeRule("C", true, true)));

        List<RuleResult> results = engine.evaluate(TestData.ordinaryTx());

        assertEquals(List.of("A", "C"), results.stream().map(RuleResult::ruleCode).toList());
    }

    @Test
    void disabledRulesAreSkipped() {
        FraudEngine engine = new FraudEngine(List.of(new FakeRule("A", false, true)));

        assertTrue(engine.evaluate(TestData.ordinaryTx()).isEmpty());
    }

    @Test
    void cleanTransaction_returnsEmptyList() {
        FraudEngine engine = new FraudEngine(List.of(new FakeRule("A", true, false)));

        assertTrue(engine.evaluate(TestData.ordinaryTx()).isEmpty());
    }
}
