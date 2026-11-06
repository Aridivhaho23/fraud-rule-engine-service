package com.example.fraud.entity;

/**
 * The life cycle of a fraud flag as an analyst works on it.
 *
 * <pre>
 *   OPEN --> UNDER_REVIEW --> CONFIRMED_FRAUD  (final)
 *     |            |------> FALSE_POSITIVE   (final)
 *     +---------------------^ (an analyst may also decide straight away)
 * </pre>
 * The allowed moves are encoded here, in ONE place, so no other class can invent an illegal one.
 */
public enum FlagStatus {
    OPEN,
    UNDER_REVIEW,
    CONFIRMED_FRAUD,
    FALSE_POSITIVE;

    /** @return true when moving from this status to {@code target} is allowed. */
    public boolean canTransitionTo(FlagStatus target) {
        return switch (this) {
            case OPEN -> target == UNDER_REVIEW || target == CONFIRMED_FRAUD || target == FALSE_POSITIVE;
            case UNDER_REVIEW -> target == CONFIRMED_FRAUD || target == FALSE_POSITIVE;
            case CONFIRMED_FRAUD, FALSE_POSITIVE -> false; // final states
        };
    }

    public boolean isFinal() {
        return this == CONFIRMED_FRAUD || this == FALSE_POSITIVE;
    }
}
