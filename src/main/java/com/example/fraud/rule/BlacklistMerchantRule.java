package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.BlacklistMerchant;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import com.example.fraud.repository.BlacklistMerchantRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** BLACKLIST_MERCHANT - any transaction at a merchant on the (active) blacklist is CRITICAL. */
@Component
public class BlacklistMerchantRule implements FraudRule {

    public static final String CODE = "BLACKLIST_MERCHANT";

    private final FraudRulesProperties.Blacklist config;
    private final BlacklistMerchantRepository blacklistRepository;

    public BlacklistMerchantRule(FraudRulesProperties properties, BlacklistMerchantRepository blacklistRepository) {
        this.config = properties.blacklist();
        this.blacklistRepository = blacklistRepository;
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
        String key = BlacklistMerchant.keyOf(tx.getMerchantName());
        if (blacklistRepository.existsByMerchantKeyAndRemovedAtIsNull(key)) {
            return Optional.of(new RuleResult(CODE, Severity.CRITICAL, 100,
                    "Merchant '" + tx.getMerchantName() + "' is on the fraud blacklist"));
        }
        return Optional.empty();
    }
}
