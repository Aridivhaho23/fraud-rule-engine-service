package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import com.example.fraud.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardTestingRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    private final LocalDateTime when = LocalDateTime.of(2026, 10, 20, 10, 0);

    @Test
    void normalSizedTransaction_isIgnored() {
        CardTestingRule rule = new CardTestingRule(TestData.defaultRules(), transactionRepository);

        assertTrue(rule.evaluate(TestData.tx("150.00", MerchantCategory.RETAIL, "Shop", "ZA", when)).isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void twoSmallTransactions_isNotFlagged() {
        when(transactionRepository.countByCustomerIdAndAmountLessThanEqualAndOccurredAtBetween(
                eq(1L), eq(new BigDecimal("20")), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        CardTestingRule rule = new CardTestingRule(TestData.defaultRules(), transactionRepository);

        assertTrue(rule.evaluate(TestData.tx("10.00", MerchantCategory.OTHER, "Web Shop", "US", when)).isEmpty());
    }

    @Test
    void threeSmallTransactions_isHigh() {
        when(transactionRepository.countByCustomerIdAndAmountLessThanEqualAndOccurredAtBetween(
                eq(1L), eq(new BigDecimal("20")), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        CardTestingRule rule = new CardTestingRule(TestData.defaultRules(), transactionRepository);

        RuleResult result = rule.evaluate(TestData.tx("10.00", MerchantCategory.OTHER, "Web Shop", "US", when)).orElseThrow();

        assertEquals(Severity.HIGH, result.severity());
        assertEquals(CardTestingRule.CODE, result.ruleCode());
    }
}
