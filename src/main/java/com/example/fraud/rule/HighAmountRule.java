package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * HIGH_AMOUNT - a single unusually large transaction.
 * At or above the threshold: HIGH. At or above the critical threshold: CRITICAL.
 */
@Component
public class HighAmountRule implements FraudRule {

    public static final String CODE = "HIGH_AMOUNT";

    private final FraudRulesProperties.HighAmount config;

    public HighAmountRule(FraudRulesProperties properties) {
        this.config = properties.highAmount();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    public Optional<RuleResult> evaluate(Transaction tx) {
        if (tx.getAmount().compareTo(config.criticalThreshold()) >= 0) {
            return Optional.of(new RuleResult(CODE, Severity.CRITICAL, 95,
                    "Amount R" + tx.getAmount() + " is at or above the critical threshold of R" + config.criticalThreshold()));
        }
        if (tx.getAmount().compareTo(config.threshold()) >= 0) {
            return Optional.of(new RuleResult(CODE, Severity.HIGH, 75,
                    "Amount R" + tx.getAmount() + " is at or above the high-amount threshold of R" + config.threshold()));
        }
        return Optional.empty();
    }
}
