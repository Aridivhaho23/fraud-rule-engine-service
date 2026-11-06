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

/** The repository is a Mockito mock, so we control "how many recent transactions exist". */
@ExtendWith(MockitoExtension.class)
class VelocityRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    private VelocityRule rule() {
        return new VelocityRule(TestData.defaultRules(), transactionRepository);
    }

    @Test
    void fiveTransactionsInWindow_isNotFlagged() {
        when(transactionRepository.countByCustomerIdAndOccurredAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        assertTrue(rule().evaluate(TestData.ordinaryTx()).isEmpty());
    }

    @Test
    void sixTransactionsInWindow_isHigh() {
        when(transactionRepository.countByCustomerIdAndOccurredAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(6L);

        RuleResult result = rule().evaluate(TestData.ordinaryTx()).orElseThrow();

        assertEquals(Severity.HIGH, result.severity());
        assertEquals(VelocityRule.CODE, result.ruleCode());
    }
}
