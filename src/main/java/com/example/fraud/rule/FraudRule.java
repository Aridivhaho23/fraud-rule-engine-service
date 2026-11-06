package com.example.fraud.rule;

import com.example.fraud.entity.Transaction;
import java.util.Optional;

/**
 * THE central abstraction of the engine: one fraud rule.
 *
 * <p><b>Strategy pattern / open-closed principle.</b> Every rule is a separate class implementing this
 * interface and registered as a Spring bean. {@link FraudEngine} receives ALL of them automatically.
 * To add a new rule you write ONE new class - no existing class changes.
 *
 * <p>A rule must be <b>stateless</b> and must not change the transaction; it only inspects it (and
 * possibly the customer's recent history) and either returns a {@link RuleResult} or nothing.
 */
public interface FraudRule {

    /** Stable identifier stored on flags, e.g. {@code HIGH_AMOUNT}. */
    String code();

    /** Rules can be switched off in configuration ({@code fraud.rules.<name>.enabled=false}). */
    boolean isEnabled();

    /**
     * @return a result when the transaction looks suspicious to this rule, otherwise empty
     */
    Optional<RuleResult> evaluate(Transaction transaction);
}
