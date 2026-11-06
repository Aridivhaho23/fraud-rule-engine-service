package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import com.example.fraud.repository.BlacklistMerchantRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlacklistMerchantRuleTest {

    @Mock
    private BlacklistMerchantRepository blacklistRepository;

    private final LocalDateTime when = LocalDateTime.of(2026, 10, 20, 10, 0);

    @Test
    void blacklistedMerchant_isCritical_andMatchingIgnoresCaseAndSpaces() {
        when(blacklistRepository.existsByMerchantKeyAndRemovedAtIsNull("darkbet offshore")).thenReturn(true);
        BlacklistMerchantRule rule = new BlacklistMerchantRule(TestData.defaultRules(), blacklistRepository);

        RuleResult result = rule.evaluate(
                TestData.tx("200.00", MerchantCategory.GAMBLING, "  DarkBet OFFSHORE ", "MT", when)).orElseThrow();

        assertEquals(Severity.CRITICAL, result.severity());
        assertEquals(100, result.riskScore());
    }

    @Test
    void unknownMerchant_isNotFlagged() {
        when(blacklistRepository.existsByMerchantKeyAndRemovedAtIsNull("corner shop")).thenReturn(false);
        BlacklistMerchantRule rule = new BlacklistMerchantRule(TestData.defaultRules(), blacklistRepository);

        assertTrue(rule.evaluate(TestData.ordinaryTx()).isEmpty());
    }
}
