package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CARD_TESTING - criminals who steal card details first make several tiny purchases to check the card
 * works. If the current transaction is tiny (default R20 or less) and the customer has made at least
 * N such tiny transactions within the window (default 3 in 10 minutes, including this one), it is flagged.
 */
@Component
public class CardTestingRule implements FraudRule {

    public static final String CODE = "CARD_TESTING";

    private final FraudRulesProperties.CardTesting config;
    private final TransactionRepository transactionRepository;

    public CardTestingRule(FraudRulesProperties properties, TransactionRepository transactionRepository) {
        this.config = properties.cardTesting();
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
        if (tx.getAmount().compareTo(config.smallAmount()) > 0) {
            return Optional.empty(); // only tiny transactions are "test" transactions
        }
        LocalDateTime to = tx.getOccurredAt();
        LocalDateTime from = to.minus(config.window());
        long smallCount = transactionRepository.countByCustomerIdAndAmountLessThanEqualAndOccurredAtBetween(
                tx.getCustomer().getId(), config.smallAmount(), from, to);
        if (smallCount >= config.minAttempts()) {
            return Optional.of(new RuleResult(CODE, Severity.HIGH, 80,
                    smallCount + " small transactions (each R" + config.smallAmount() + " or less) within "
                            + config.window().toMinutes() + " minutes - possible card testing"));
        }
        return Optional.empty();
    }
}
