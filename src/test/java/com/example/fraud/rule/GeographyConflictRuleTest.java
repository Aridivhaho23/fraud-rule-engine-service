package com.example.fraud.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.fraud.entity.Severity;
import com.example.fraud.repository.TransactionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeographyConflictRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void noTransactionInAnotherCountry_isNotFlagged() {
        when(transactionRepository.existsByCustomerIdAndCountryNotAndIdNotAndOccurredAtBetween(
                eq(1L), eq("ZA"), eq(100L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);

        GeographyConflictRule rule = new GeographyConflictRule(TestData.defaultRules(), transactionRepository);

        assertTrue(rule.evaluate(TestData.ordinaryTx()).isEmpty());
    }

    @Test
    void transactionInAnotherCountryWithinWindow_isHigh() {
        when(transactionRepository.existsByCustomerIdAndCountryNotAndIdNotAndOccurredAtBetween(
                eq(1L), eq("ZA"), eq(100L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);

        GeographyConflictRule rule = new GeographyConflictRule(TestData.defaultRules(), transactionRepository);
        RuleResult result = rule.evaluate(TestData.ordinaryTx()).orElseThrow();

        assertEquals(Severity.HIGH, result.severity());
        assertEquals(GeographyConflictRule.CODE, result.ruleCode());
    }
}
