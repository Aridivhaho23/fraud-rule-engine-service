package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * VELOCITY - too many transactions in a short time (default: more than 5 in 10 minutes).
 * The count INCLUDES the transaction being evaluated, because it is saved before the rules run.
 */
@Component
public class VelocityRule implements FraudRule {

    public static final String CODE = "VELOCITY";

    private final FraudRulesProperties.Velocity config;
    private final TransactionRepository transactionRepository;

    public VelocityRule(FraudRulesProperties properties, TransactionRepository transactionRepository) {
        this.config = properties.velocity();
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
        LocalDateTime to = tx.getOccurredAt();
        LocalDateTime from = to.minus(config.window());
        long count = transactionRepository.countByCustomerIdAndOccurredAtBetween(tx.getCustomer().getId(), from, to);
        if (count > config.maxTransactions()) {
            return Optional.of(new RuleResult(CODE, Severity.HIGH, 70,
                    count + " transactions within " + config.window().toMinutes() + " minutes (limit "
                            + config.maxTransactions() + ")"));
        }
        return Optional.empty();
    }
}
