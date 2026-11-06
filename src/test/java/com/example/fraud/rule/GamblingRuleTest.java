package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GamblingRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    private final LocalDateTime when = LocalDateTime.of(2026, 10, 20, 10, 0);

    private GamblingRule rule(long recentGamblingCount) {
        lenient().when(transactionRepository.countByCustomerIdAndMerchantCategoryAndOccurredAtBetween(
                eq(1L), eq(MerchantCategory.GAMBLING), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(recentGamblingCount);
        return new GamblingRule(TestData.defaultRules(), transactionRepository);
    }

    @Test
    void nonGamblingTransaction_isIgnoredWithoutQueryingTheDatabase() {
        GamblingRule rule = new GamblingRule(TestData.defaultRules(), transactionRepository);

        assertTrue(rule.evaluate(TestData.tx("9000.00", MerchantCategory.RETAIL, "Shop", "ZA", when)).isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void smallSingleGamblingTransaction_isNotFlagged() {
        assertTrue(rule(1).evaluate(TestData.tx("100.00", MerchantCategory.GAMBLING, "Casino", "ZA", when)).isEmpty());
    }

    @Test
    void largeGamblingTransaction_isMedium() {
        RuleResult result = rule(1).evaluate(TestData.tx("5000.00", MerchantCategory.GAMBLING, "Casino", "ZA", when)).orElseThrow();

        assertEquals(Severity.MEDIUM, result.severity());
    }

    @Test
    void repeatedGambling_isMedium() {
        RuleResult result = rule(3).evaluate(TestData.tx("100.00", MerchantCategory.GAMBLING, "Casino", "ZA", when)).orElseThrow();

        assertEquals(Severity.MEDIUM, result.severity());
    }

    @Test
    void largeAndRepeated_isHigh() {
        RuleResult result = rule(4).evaluate(TestData.tx("8000.00", MerchantCategory.GAMBLING, "Casino", "ZA", when)).orElseThrow();

        assertEquals(Severity.HIGH, result.severity());
    }
}
