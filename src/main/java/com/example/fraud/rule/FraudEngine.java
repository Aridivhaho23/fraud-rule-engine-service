package com.example.fraud.rule;

import com.example.fraud.entity.Transaction;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs a transaction through every enabled {@link FraudRule} and collects what fired.
 *
 * <p>Spring injects a {@code List<FraudRule>} containing EVERY bean that implements the interface, so
 * the engine never mentions a concrete rule. Adding a rule = adding a class.
 *
 * <p><b>Design decision - fail loudly.</b> If a rule throws, the exception is NOT swallowed: the whole
 * ingestion fails (and is rolled back) rather than silently skipping a rule, because a silent gap in
 * fraud screening is worse than a visible error. A production variant might instead record the failure
 * and flag the transaction for manual review.
 */
@Component
public class FraudEngine {

    private static final Logger log = LoggerFactory.getLogger(FraudEngine.class);

    private final List<FraudRule> rules;

    public FraudEngine(List<FraudRule> rules) {
        this.rules = List.copyOf(rules);
        log.info("Fraud engine started with {} rules: {}", rules.size(),
                rules.stream().map(r -> r.code() + (r.isEnabled() ? "" : "(disabled)")).toList());
    }

    /** @return the results of all rules that fired for this transaction (empty list = looks clean) */
    public List<RuleResult> evaluate(Transaction transaction) {
        List<RuleResult> results = new ArrayList<>();
        for (FraudRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            rule.evaluate(transaction).ifPresent(results::add);
        }
        return results;
    }
}
