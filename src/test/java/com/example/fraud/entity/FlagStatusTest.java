package com.example.fraud.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fraud.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** The flag life cycle: allowed and forbidden status changes. */
class FlagStatusTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 10, 20, 12, 0);

    private FraudFlag newFlag() {
        Customer customer = new Customer("A", "a@example.com", "ZA", now);
        Transaction tx = new Transaction(customer, "REF", new BigDecimal("100"), "Shop",
                MerchantCategory.RETAIL, "ZA", now, now);
        return new FraudFlag(tx, "HIGH_AMOUNT", Severity.HIGH, 75, "test", now);
    }

    @Test
    void openFlag_canBeReviewedThenConfirmed() {
        FraudFlag flag = newFlag();

        flag.transitionTo(FlagStatus.UNDER_REVIEW, "analyst", "looking", now);
        flag.transitionTo(FlagStatus.CONFIRMED_FRAUD, "analyst", "customer confirmed", now);

        assertEquals(FlagStatus.CONFIRMED_FRAUD, flag.getStatus());
        assertEquals("analyst", flag.getReviewedBy());
    }

    @Test
    void finalStatus_cannotBeChanged() {
        FraudFlag flag = newFlag();
        flag.transitionTo(FlagStatus.FALSE_POSITIVE, "analyst", null, now);

        assertThrows(BusinessRuleException.class,
                () -> flag.transitionTo(FlagStatus.OPEN, "analyst", null, now));
        assertEquals(FlagStatus.FALSE_POSITIVE, flag.getStatus());
    }

    @Test
    void cannotMoveBackToOpen() {
        FraudFlag flag = newFlag();
        flag.transitionTo(FlagStatus.UNDER_REVIEW, "analyst", null, now);

        assertThrows(BusinessRuleException.class,
                () -> flag.transitionTo(FlagStatus.OPEN, "analyst", null, now));
    }

    @Test
    void transitionTable() {
        assertTrue(FlagStatus.OPEN.canTransitionTo(FlagStatus.UNDER_REVIEW));
        assertTrue(FlagStatus.UNDER_REVIEW.canTransitionTo(FlagStatus.FALSE_POSITIVE));
        assertFalse(FlagStatus.UNDER_REVIEW.canTransitionTo(FlagStatus.OPEN));
        assertTrue(FlagStatus.CONFIRMED_FRAUD.isFinal());
    }
}
