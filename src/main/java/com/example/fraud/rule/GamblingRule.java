package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * GAMBLING - high-risk spend category. Fires for gambling transactions that are either LARGE
 * (default R5,000 or more) or REPEATED (default 3 or more in 24 hours). Both together raise it to HIGH.
 * Relies on the transaction already being categorised.
 */
@Component
public class GamblingRule implements FraudRule {

    public static final String CODE = "GAMBLING";

    private final FraudRulesProperties.Gambling config;
    private final TransactionRepository transactionRepository;

    public GamblingRule(FraudRulesProperties properties, TransactionRepository transactionRepository) {
        this.config = properties.gambling();
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
        if (tx.getMerchantCategory() != MerchantCategory.GAMBLING) {
            return Optional.empty();
        }
        boolean large = tx.getAmount().compareTo(config.largeAmount()) >= 0;

        LocalDateTime to = tx.getOccurredAt();
        LocalDateTime from = to.minus(config.repeatWindow());
        long recent = transactionRepository.countByCustomerIdAndMerchantCategoryAndOccurredAtBetween(
                tx.getCustomer().getId(), MerchantCategory.GAMBLING, from, to);
        boolean repeated = recent >= config.repeatCount();

        if (!large && !repeated) {
            return Optional.empty();
        }
        Severity severity = (large && repeated) ? Severity.HIGH : Severity.MEDIUM;
        int score = (large && repeated) ? 70 : 55;
        StringBuilder reason = new StringBuilder("Gambling transaction");
        if (large) {
            reason.append(": large amount R").append(tx.getAmount());
        }
        if (repeated) {
            reason.append(large ? " and " : ": ").append(recent).append(" gambling transactions in ")
                    .append(config.repeatWindow().toHours()).append(" hours");
        }
        return Optional.of(new RuleResult(CODE, severity, score, reason.toString()));
    }
}
