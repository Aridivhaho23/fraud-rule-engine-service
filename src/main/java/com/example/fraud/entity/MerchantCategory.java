package com.example.fraud.entity;

/**
 * The kind of business a transaction was made at.
 * Transactions arrive already CATEGORISED (the upstream aggregation system assigns this), which is
 * what lets rules such as {@code GamblingRule} work without guessing from the merchant name.
 */
public enum MerchantCategory {
    GROCERIES,
    FUEL,
    RETAIL,
    RESTAURANTS,
    TRAVEL,
    ENTERTAINMENT,
    UTILITIES,
    ATM_WITHDRAWAL,
    GAMBLING,
    OTHER
}
