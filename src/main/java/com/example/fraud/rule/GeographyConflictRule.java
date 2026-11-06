package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * GEO_CONFLICT - "impossible travel": the same customer transacts in two different countries within a
 * short window (default 60 minutes). The window is checked in BOTH directions so that transactions
 * arriving out of order are still caught.
 */
@Component
public class GeographyConflictRule implements FraudRule {

    public static final String CODE = "GEO_CONFLICT";

    private final FraudRulesProperties.Geography config;
    private final TransactionRepository transactionRepository;

    public GeographyConflictRule(FraudRulesProperties properties, TransactionRepository transactionRepository) {
        this.config = properties.geography();
        this.transactionRepository = transactionRepository;
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
        LocalDateTime from = tx.getOccurredAt().minus(config.window());
        LocalDateTime to = tx.getOccurredAt().plus(config.window());
        boolean conflict = transactionRepository.existsByCustomerIdAndCountryNotAndIdNotAndOccurredAtBetween(
                tx.getCustomer().getId(), tx.getCountry(), tx.getId(), from, to);
        if (conflict) {
            return Optional.of(new RuleResult(CODE, Severity.HIGH, 80,
                    "Customer also transacted in a different country (this one: " + tx.getCountry()
                            + ") within " + config.window().toMinutes() + " minutes"));
        }
        return Optional.empty();
    }
}
