package com.example.fraud.rule;

import com.example.fraud.config.FraudRulesProperties;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * NIGHT_TRANSACTION - a meaningful purchase in the middle of the night (default 01:00-04:59 local time,
 * amount of at least R500). The minimum amount avoids flooding analysts with R20 petrol-station purchases.
 * Times are stored in UTC and converted to the configured time zone (default Africa/Johannesburg).
 */
@Component
public class NightTransactionRule implements FraudRule {

    public static final String CODE = "NIGHT_TRANSACTION";

    private final FraudRulesProperties.Night config;
    private final ZoneId zone;

    public NightTransactionRule(FraudRulesProperties properties) {
        this.config = properties.night();
        this.zone = ZoneId.of(config.zone());
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
        int localHour = tx.getOccurredAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).getHour();
        boolean night = localHour >= config.startHour() && localHour < config.endHour();
        if (night && tx.getAmount().compareTo(config.minAmount()) >= 0) {
            return Optional.of(new RuleResult(CODE, Severity.MEDIUM, 50,
                    "Transaction of R" + tx.getAmount() + " at " + localHour + ":xx local time (" + config.zone()
                            + "), inside the night window " + config.startHour() + ":00-" + config.endHour() + ":00"));
        }
        return Optional.empty();
    }
}
